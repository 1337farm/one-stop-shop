use std::env;
use std::sync::Arc;
use tokio::net::TcpListener;
use tokio_tungstenite::accept_async;
use futures_util::{StreamExt, SinkExt};
use serde::{Deserialize, Serialize};
use serde_json::Value;

use rig::providers::openai::Client;
use rig::completion::Prompt;
use tools::BashExecutor;
use wasm::WasmTransformer;
use memory::MemoryEngine;

mod tools;
mod wasm;
mod memory;

#[derive(Serialize, Deserialize, Debug)]
struct RpcRequest {
    jsonrpc: String,
    method: String,
    #[serde(default)]
    params: Option<Value>,
    #[serde(default)]
    id: Option<Value>,
}

#[derive(Serialize, Deserialize, Debug)]
struct RpcResponse {
    jsonrpc: String,
    result: Option<Value>,
    error: Option<RpcError>,
    id: Option<Value>,
}

#[derive(Serialize, Deserialize, Debug)]
struct RpcError {
    code: i32,
    message: String,
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    tracing_subscriber::fmt::init();

    let port = env::var("PORT").unwrap_or_else(|_| "8080".to_string());
    let addr = format!("127.0.0.1:{}", port);

    let openai_api_key = env::var("OPENAI_API_KEY").unwrap_or_else(|_| "dummy-key".to_string());
    let openai_client = Client::new(&openai_api_key);

    let agent = openai_client
        .agent("gpt-4")
        .preamble("You are an autonomous orchestrator daemon running on a Linux userland.")
        .tool(BashExecutor::default())
        .tool(WasmTransformer::default())
        .build();
    let agent = Arc::new(agent);

    let memory_engine = Arc::new(MemoryEngine::new("oss_memory.db").await?);

    println!("Listening on: {}", addr);
    let listener = TcpListener::bind(&addr).await?;

    while let Ok((stream, _)) = listener.accept().await {
        let agent = Arc::clone(&agent);
        let memory = Arc::clone(&memory_engine);

        tokio::spawn(async move {
            let ws_stream = accept_async(stream).await.expect("Error during the websocket handshake occurred");
            println!("New WebSocket connection");

            let (mut ws_sender, mut ws_receiver) = ws_stream.split();

            while let Some(msg) = ws_receiver.next().await {
                if let Ok(msg) = msg {
                    if msg.is_text() {
                        let text = msg.to_text().unwrap();
                        println!("Received: {}", text);

                        let response = match serde_json::from_str::<RpcRequest>(text) {
                            Ok(req) => {
                                println!("Parsed JSON-RPC request: {:?}", req);

                                if req.method == "chat" {
                                    if let Some(params) = &req.params {
                                        if let Some(prompt) = params.get("prompt").and_then(|p| p.as_str()) {
                                            match agent.prompt(prompt).await {
                                                Ok(completion) => {
                                                    let completion_val = serde_json::json!(completion);
                                                    if let Err(e) = memory.log_trace(prompt, &completion_val).await {
                                                        eprintln!("Failed to log trace to memory: {}", e);
                                                    }

                                                    RpcResponse {
                                                        jsonrpc: "2.0".to_string(),
                                                        result: Some(completion_val),
                                                        error: None,
                                                        id: req.id,
                                                    }
                                                }
                                                Err(e) => {
                                                    RpcResponse {
                                                        jsonrpc: "2.0".to_string(),
                                                        result: None,
                                                        error: Some(RpcError {
                                                            code: -32603,
                                                            message: format!("Agent error: {}", e),
                                                        }),
                                                        id: req.id,
                                                    }
                                                }
                                            }
                                        } else {
                                            RpcResponse {
                                                jsonrpc: "2.0".to_string(),
                                                result: None,
                                                error: Some(RpcError {
                                                    code: -32602,
                                                    message: "Missing 'prompt' in params".to_string(),
                                                }),
                                                id: req.id,
                                            }
                                        }
                                    } else {
                                        RpcResponse {
                                            jsonrpc: "2.0".to_string(),
                                            result: None,
                                            error: Some(RpcError {
                                                code: -32602,
                                                message: "Missing params".to_string(),
                                            }),
                                            id: req.id,
                                        }
                                    }
                                } else {
                                    RpcResponse {
                                        jsonrpc: "2.0".to_string(),
                                        result: None,
                                        error: Some(RpcError {
                                            code: -32601,
                                            message: "Method not found".to_string(),
                                        }),
                                        id: req.id,
                                    }
                                }
                            }
                            Err(e) => {
                                println!("Failed to parse JSON-RPC: {}", e);
                                RpcResponse {
                                    jsonrpc: "2.0".to_string(),
                                    result: None,
                                    error: Some(RpcError {
                                        code: -32700,
                                        message: "Parse error".to_string(),
                                    }),
                                    id: None,
                                }
                            }
                        };

                        let response_str = serde_json::to_string(&response).unwrap();
                        if let Err(e) = ws_sender.send(tokio_tungstenite::tungstenite::Message::Text(response_str)).await {
                            println!("Error sending message: {}", e);
                            break;
                        }
                    }
                }
            }
        });
    }

    Ok(())
}

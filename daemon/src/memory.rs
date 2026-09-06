use tokio_rusqlite::Connection;
use std::sync::Arc;
use serde_json::Value;
use tokio::process::Command;

#[derive(Clone)]
pub struct MemoryEngine {
    db: Arc<Connection>,
}

impl MemoryEngine {
    pub async fn new(db_path: &str) -> Result<Self, Box<dyn std::error::Error>> {
        let conn = Connection::open(db_path).await?;

        // Initialize schema
        conn.call(|conn| {
            conn.execute(
                "CREATE TABLE IF NOT EXISTS execution_traces (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    prompt TEXT NOT NULL,
                    completion TEXT NOT NULL
                )",
                [],
            )?;

            conn.execute(
                "CREATE TABLE IF NOT EXISTS macro_memory (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    milestone TEXT NOT NULL,
                    context TEXT NOT NULL
                )",
                [],
            )?;
            Ok(())
        }).await?;

        Ok(Self {
            db: Arc::new(conn),
        })
    }

    pub async fn log_trace(&self, prompt: &str, completion: &Value) -> Result<(), Box<dyn std::error::Error>> {
        let prompt_str = prompt.to_string();
        let completion_str = serde_json::to_string(completion)?;

        self.db.call(move |conn| {
            conn.execute(
                "INSERT INTO execution_traces (prompt, completion) VALUES (?1, ?2)",
                (&prompt_str, &completion_str),
            )?;
            Ok(())
        }).await?;
        Ok(())
    }
}

pub struct HeuristicEvaluator;

impl HeuristicEvaluator {
    pub async fn evaluate_trace(_trace: &str) -> String {
        // Stub for local llama.cpp invocation
        let output = Command::new("llama-cli")
            .arg("--version")
            .output()
            .await;

        match output {
            Ok(_) => {
                // Return a mocked success for now if it exists, normally we'd run the model
                "Evaluated: Milestone not reached (llama-cli exists)".to_string()
            }
            Err(_) => {
                // Fallback response if llama-cli isn't installed
                "Evaluated fallback: llama-cli missing, milestone evaluation skipped.".to_string()
            }
        }
    }
}

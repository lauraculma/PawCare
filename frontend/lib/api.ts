const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api";

export type ApiHealth = {
  status: string;
  service: string;
};

export async function getHealth(): Promise<ApiHealth> {
  const response = await fetch(`${API_URL}/health`, { cache: "no-store" });

  if (!response.ok) {
    throw new Error(`API responded with status ${response.status}`);
  }

  return response.json();
}

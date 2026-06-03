import { useState } from 'react';
import type { FormEvent } from 'react';
import './App.css';
import { Header } from './components/Header';
import { PromptForm } from './components/PromptForm';
import { ErrorMessage } from './components/ErrorMessage';
import { ResultsDisplay } from './components/ResultsDisplay';
import type {OptimizationResponse} from './types';

// Use environment variable for Prod, fallback to relative path (proxy) for Dev
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

function App() {
  const [prompt, setPrompt] = useState<string>('');
  const [result, setResult] = useState<OptimizationResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!prompt.trim()) return;

    setLoading(true);
    setError(null);
    setResult(null);

    // Construct the full URL
    // If API_BASE_URL is '/api', result is '/api/prompt/optimize' (proxied locally)
    // If API_BASE_URL is 'https://backend.railway.app', result is 'https://backend.railway.app/prompt/optimize'
    const url = `${API_BASE_URL}/prompt/optimize`;
    console.log('Fetching URL:', url);

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'text/plain',
        },
        body: prompt,
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(
            `Failed to optimize prompt: ${response.status} ${response.statusText} - ${errorText}`);
      }

      const data: OptimizationResponse = await response.json();
      setResult(data);
    } catch (err) {
      console.error('Full Error Details:', err);
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('An unknown error occurred');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <div className="min-h-screen bg-[#0f172a] text-gray-100 py-12 px-4 selection:bg-purple-500 selection:text-white font-sans">
        {/* Background Glow Effects */}
        <div className="fixed top-0 left-0 w-full h-full overflow-hidden -z-10 pointer-events-none">
          <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-purple-600/20 rounded-full blur-[120px] animate-pulse-slow"></div>
          <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-blue-600/20 rounded-full blur-[120px] animate-pulse-slow delay-1000"></div>
        </div>

        <div className="max-w-5xl mx-auto">
          <Header />
          <PromptForm 
            prompt={prompt} 
            setPrompt={setPrompt} 
            handleSubmit={handleSubmit} 
            loading={loading} 
          />
          {error && <ErrorMessage error={error} />}
          {result && <ResultsDisplay result={result} />}
        </div>
      </div>
    </>
  );
}

export default App;
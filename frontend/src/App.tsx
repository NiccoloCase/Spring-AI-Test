import { enqueueSnackbar } from "notistack";
import { useState } from "react";

const DEV_SERVER_URL = "http://localhost:3002";

interface EvaluationResponse {
  taskResponse: number;
  coherenceCohesion: number;
  lexicalResource: number;
  grammaticalRangeAccuracy: number;
  overallBand: number;
  examinerFeedback: string;
  suggestions: Record<string, string>;
}

export default function Home() {
  const [essay, setEssay] = useState<string>("");
  const [question, setQuestion] = useState<string>("");
  const [feedback, setFeedback] = useState<EvaluationResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!essay.trim()) {
      enqueueSnackbar("Please enter your essay text", { variant: "warning" });
      return;
    }

    setLoading(true);
    setFeedback(null);

    try {
      const body = { essay, question: question, taskType: "2" };

      const response = await fetch(DEV_SERVER_URL + "/ai/scoreEssay", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });

      if (!response.ok) {
        if (response.status === 422) {
          enqueueSnackbar("Please enter a valid essay text", {
            variant: "warning",
          });
          return;
        } else
          throw new Error(`Server responded with status ${response.status}`);
      }

      const data: EvaluationResponse = await response.json();
      console.log("Feedback data:", data);

      setFeedback(data);
    } catch (error) {
      enqueueSnackbar("Error fetching feedback. Please try again later.", {
        variant: "error",
      });
      console.error("Error fetching feedback:", error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100 dark:bg-gray-900 p-6">
      <div className="bg-white dark:bg-gray-800 shadow-lg rounded-lg p-6 max-w-3xl w-full transition">
        <h1 className="text-2xl font-bold text-gray-800 dark:text-gray-200 mb-4">
          IELTS Essay Checker
        </h1>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label
              htmlFor="question"
              className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1"
            >
              Question
            </label>
            <input
              id="question"
              type="text"
              className="w-full p-3 border border-gray-300 dark:border-gray-600 bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              placeholder="Enter the essay question"
              required
            />
          </div>

          <div>
            <label
              htmlFor="essay"
              className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1"
            >
              Your Essay
            </label>
            <textarea
              id="essay"
              className="w-full h-40 p-3 border border-gray-300 dark:border-gray-600 bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              value={essay}
              onChange={(e) => setEssay(e.target.value)}
              placeholder="Paste your IELTS essay here..."
              required
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white py-2 px-4 rounded-lg font-semibold transition disabled:opacity-50"
          >
            {loading ? (
              <span className="flex items-center justify-center">
                <svg
                  className="animate-spin -ml-1 mr-3 h-5 w-5 text-white"
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle
                    className="opacity-25"
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    strokeWidth="4"
                  ></circle>
                  <path
                    className="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  ></path>
                </svg>
                Grading...
              </span>
            ) : (
              "Get Feedback"
            )}
          </button>
        </form>

        {feedback && (
          <div className="mt-6 p-4 bg-gray-50 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg">
            <h2 className="text-xl font-bold text-gray-800 dark:text-gray-200">
              AI Feedback
            </h2>
            <p className="text-lg font-semibold text-gray-900 dark:text-gray-300 mt-2">
              Overall Band Score: {feedback.overallBand}/9
            </p>

            <div className="mt-4 space-y-3">
              <div className="bg-white dark:bg-gray-800 p-3 rounded-lg shadow">
                <p className="font-semibold text-gray-900 dark:text-gray-300">
                  Examiner Feedback
                </p>
                <p className="text-gray-700 dark:text-gray-400 mt-1">
                  {feedback.examinerFeedback}
                </p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                <div className="bg-white dark:bg-gray-800 p-3 rounded-lg shadow">
                  <p className="font-semibold text-gray-900 dark:text-gray-300">
                    Task Response: {feedback.taskResponse}/9
                  </p>
                </div>
                <div className="bg-white dark:bg-gray-800 p-3 rounded-lg shadow">
                  <p className="font-semibold text-gray-900 dark:text-gray-300">
                    Coherence & Cohesion: {feedback.coherenceCohesion}/9
                  </p>
                </div>
                <div className="bg-white dark:bg-gray-800 p-3 rounded-lg shadow">
                  <p className="font-semibold text-gray-900 dark:text-gray-300">
                    Lexical Resource: {feedback.lexicalResource}/9
                  </p>
                </div>
                <div className="bg-white dark:bg-gray-800 p-3 rounded-lg shadow">
                  <p className="font-semibold text-gray-900 dark:text-gray-300">
                    Grammatical Range & Accuracy:{" "}
                    {feedback.grammaticalRangeAccuracy}/9
                  </p>
                </div>
              </div>

              {feedback.suggestions &&
                Object.keys(feedback.suggestions).length > 0 && (
                  <div className="bg-white dark:bg-gray-800 p-3 rounded-lg shadow">
                    <p className="font-semibold text-gray-900 dark:text-gray-300 mb-2">
                      Suggestions for Improvement
                    </p>
                    <ul className="list-disc pl-5 space-y-1 text-gray-700 dark:text-gray-400">
                      {Object.entries(feedback.suggestions).map(
                        ([key, value]) => (
                          <li key={key}>{value}</li>
                        )
                      )}
                    </ul>
                  </div>
                )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

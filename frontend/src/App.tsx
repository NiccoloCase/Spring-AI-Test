import { enqueueSnackbar } from "notistack";
import { useState, useEffect } from "react";

const DEV_SERVER_URL = "http://localhost:30002";

interface FeedbackItem {
  score: number;
  feedback: string;
}

interface FeedbackResponse {
  overall_band: number;
  [key: string]: FeedbackItem | number;
}

export default function Home() {
  const [essay, setEssay] = useState<string>("");
  const [feedback, setFeedback] = useState<FeedbackResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setFeedback(null);
    alert("Please wait while we process your essay...");

    try {
      const response = await fetch("DEV_SERVER_URL" + "/ai/scoreEssay", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          essay,
          question: "ciao",
          taskType: "2",
        }),
      });

      const data: FeedbackResponse = await response.json();
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
          <textarea
            className="w-full h-40 p-3 border border-gray-300 dark:border-gray-600 bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            value={essay}
            onChange={(e) => setEssay(e.target.value)}
            placeholder="Paste your IELTS essay here..."
          />
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
              📊 Feedback
            </h2>
            <p className="text-lg font-semibold text-gray-900 dark:text-gray-300 mt-2">
              Overall Band Score: {feedback.overall_band}/9
            </p>
            <div className="mt-3 space-y-2">
              {Object.entries(feedback).map(
                ([key, value]) =>
                  key !== "overall_band" && (
                    <div
                      key={key}
                      className="bg-white dark:bg-gray-800 p-3 rounded-lg shadow"
                    >
                      <p className="font-semibold capitalize text-gray-900 dark:text-gray-300">
                        {key.replace("_", " ")}: {(value as FeedbackItem).score}
                        /9
                      </p>
                      <p className="text-gray-700 dark:text-gray-400">
                        {(value as FeedbackItem).feedback}
                      </p>
                    </div>
                  )
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

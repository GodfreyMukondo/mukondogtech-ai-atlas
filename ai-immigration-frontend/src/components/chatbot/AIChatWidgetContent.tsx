import {
  useState,
  useRef,
  useEffect,
  KeyboardEvent,
} from "react";

import {
  Bot,
  Loader2,
  Send,
  User,
  Sparkles,
} from "lucide-react";

import { askQuestion } from "../../api/chatApi";

interface Message {
  id: number;
  role: "user" | "assistant";
  content: string;
}

const SUGGESTED_QUESTIONS = [
  "What documents are required for a work visa?",
  "How do I apply for permanent residency?",
  "Check my passport validity requirements.",
  "What are common visa rejection reasons?",
];

export default function AIChatWidgetContent() {
  const [messages, setMessages] = useState<
    Message[]
  >([
    {
      id: 1,
      role: "assistant",
      content:
        "Welcome to MukondoGTech AI. I can assist with immigration procedures, visa applications, residency permits, document verification, compliance requirements, and travel documentation.",
    },
  ]);

  const [input, setInput] =
    useState("");

  const [loading, setLoading] =
    useState(false);

  const [error, setError] =
    useState("");

  const bottomRef =
    useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({
      behavior: "smooth",
    });
  }, [messages, loading]);

  const sendMessage = async (
    customMessage?: string
  ) => {
    const prompt = (
      customMessage ?? input
    ).trim();

    if (!prompt || loading) {
      return;
    }

    const userMessage: Message = {
      id: Date.now(),
      role: "user",
      content: prompt,
    };

    setMessages((prev) => [
      ...prev,
      userMessage,
    ]);

    setInput("");
    setError("");
    setLoading(true);

    try {
      const response =
        await askQuestion(prompt);

      setMessages((prev) => [
        ...prev,
        {
          id: Date.now() + 1,
          role: "assistant",
          content:
            response.answer ||
            "I could not generate a response at this time.",
        },
      ]);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to contact the AI service."
      );
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (
    event: KeyboardEvent<HTMLInputElement>
  ) => {
    if (
      event.key === "Enter" &&
      !loading
    ) {
      event.preventDefault();
      sendMessage();
    }
  };

  return (
    <>
      {/* Messages Area */}

      <div
        className="
          flex-1
          overflow-y-auto
          bg-[#FAFBFC]
          p-4
        "
      >
        {/* Suggested Questions */}

        {messages.length === 1 && (
          <div className="mb-5">
            <div
              className="
                mb-3
                flex
                items-center
                gap-2
              "
            >
              <Sparkles
                size={14}
                className="text-[#F4B81A]"
              />

              <p
                className="
                  text-xs
                  font-semibold
                  uppercase
                  tracking-wide
                  text-[#667085]
                "
              >
                Suggested Questions
              </p>
            </div>

            <div className="flex flex-wrap gap-2">
              {SUGGESTED_QUESTIONS.map(
                (question) => (
                  <button
                    key={question}
                    onClick={() =>
                      sendMessage(question)
                    }
                    disabled={loading}
                    className="
                      rounded-full
                      border
                      border-[#E7EAF0]
                      bg-white
                      px-3
                      py-2
                      text-xs
                      text-[#475467]
                      transition-all
                      hover:border-[#F4B81A]
                      hover:bg-[#FFF8E6]
                      hover:text-[#0B1736]
                    "
                  >
                    {question}
                  </button>
                )
              )}
            </div>
          </div>
        )}

        {/* Chat Messages */}

        <div className="space-y-4">
          {messages.map((message) => (
            <div
              key={message.id}
              className={`flex gap-2 ${
                message.role === "user"
                  ? "justify-end"
                  : "justify-start"
              }`}
            >
              {message.role ===
                "assistant" && (
                <div
                  className="
                    flex
                    h-9
                    w-9
                    shrink-0
                    items-center
                    justify-center
                    rounded-xl
                    bg-gradient-to-br
                    from-[#0B1736]
                    to-[#183B6B]
                  "
                >
                  <Bot
                    size={16}
                    className="text-[#F4B81A]"
                  />
                </div>
              )}

              <div
                className={`
                  max-w-[80%]
                  rounded-2xl
                  px-4
                  py-3
                  text-sm
                  leading-relaxed
                  shadow-sm

                  ${
                    message.role ===
                    "user"
                      ? `
                        bg-[#0B1736]
                        text-white
                      `
                      : `
                        border
                        border-[#E7EAF0]
                        bg-white
                        text-[#0B1736]
                      `
                  }
                `}
              >
                {message.content}
              </div>

              {message.role ===
                "user" && (
                <div
                  className="
                    flex
                    h-9
                    w-9
                    shrink-0
                    items-center
                    justify-center
                    rounded-xl
                    bg-[#EEF2F7]
                  "
                >
                  <User
                    size={16}
                    className="text-[#0B1736]"
                  />
                </div>
              )}
            </div>
          ))}

          {loading && (
            <div className="flex gap-2">
              <div
                className="
                  flex
                  h-9
                  w-9
                  items-center
                  justify-center
                  rounded-xl
                  bg-gradient-to-br
                  from-[#0B1736]
                  to-[#183B6B]
                "
              >
                <Bot
                  size={16}
                  className="text-[#F4B81A]"
                />
              </div>

              <div
                className="
                  flex
                  items-center
                  gap-2
                  rounded-2xl
                  border
                  border-[#E7EAF0]
                  bg-white
                  px-4
                  py-3
                  text-sm
                  text-[#667085]
                "
              >
                <Loader2
                  size={16}
                  className="animate-spin"
                />

                Thinking...
              </div>
            </div>
          )}

          {error && (
            <div
              className="
                rounded-xl
                border
                border-red-200
                bg-red-50
                p-3
                text-sm
                text-red-600
              "
            >
              {error}
            </div>
          )}

          <div ref={bottomRef} />
        </div>
      </div>

      {/* Input Area */}

      <div
        className="
          border-t
          border-[#E7EAF0]
          bg-white
          p-4
        "
      >
        <div className="flex gap-2">
          <input
            value={input}
            onChange={(e) =>
              setInput(e.target.value)
            }
            onKeyDown={handleKeyDown}
            placeholder="Ask about visas, permits, residency..."
            disabled={loading}
            className="
              flex-1
              rounded-xl
              border
              border-[#D8DFEA]
              px-4
              py-3
              text-sm
              outline-none
              transition
              focus:border-[#F4B81A]
              focus:ring-2
              focus:ring-[#F4B81A]/20
              disabled:bg-gray-50
            "
          />

          <button
            onClick={() =>
              sendMessage()
            }
            disabled={
              loading ||
              !input.trim()
            }
            className="
              flex
              h-12
              w-12
              items-center
              justify-center
              rounded-xl
              bg-[#0B1736]
              text-white
              transition
              hover:bg-[#183B6B]
              disabled:cursor-not-allowed
              disabled:opacity-50
            "
          >
            <Send size={16} />
          </button>
        </div>

        <p
          className="
            mt-3
            text-center
            text-[11px]
            text-[#98A2B3]
          "
        >
          AI-generated guidance should be
          reviewed against official immigration
          requirements.
        </p>
      </div>
    </>
  );
}
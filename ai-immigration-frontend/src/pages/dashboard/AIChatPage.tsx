
import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type KeyboardEvent,
} from "react";

import { AnimatePresence, motion } from "framer-motion";

import {
  Bot,
  CheckCircle2,
  Clock3,
  FileSearch,
  Globe,
  Loader2,
  MessageSquare,
  Send,
  ShieldCheck,
  Sparkles,
  Trash2,
  User,
} from "lucide-react";

import { chatWithAI } from "../../api/chatApi";

interface Message {
  id: number;
  role: "user" | "assistant";
  content: string;
  timestamp: string;
}

const SUGGESTED_PROMPTS = [
  "What documents are required for a work visa?",
  "Review my passport requirements.",
  "Explain residency permit eligibility.",
  "What are common immigration compliance risks?",
];

const INITIAL_MESSAGE: Message = {
  id: 1,
  role: "assistant",
  timestamp: new Date().toLocaleTimeString(),
  content:
    "Welcome to MukondoGTech AI Immigration Intelligence. I can assist with immigration requirements, document verification, visa guidance, residency applications, compliance reviews, travel permits, and immigration policy information.",
};

export default function AIChatPage() {
  const [messages, setMessages] = useState<Message[]>([
    INITIAL_MESSAGE,
  ]);

  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const bottomRef = useRef<HTMLDivElement | null>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({
      behavior: "smooth",
    });
  }, [messages, loading]);

  const totalMessages = useMemo(
    () => messages.length,
    [messages]
  );

  const sendMessage = async (customPrompt?: string) => {
    const question = (customPrompt ?? input).trim();

    if (!question || loading) {
      return;
    }

    const userMessage: Message = {
      id: Date.now(),
      role: "user",
      content: question,
      timestamp: new Date().toLocaleTimeString(),
    };

    setMessages((previous) => [
      ...previous,
      userMessage,
    ]);

    setInput("");
    setError("");
    setLoading(true);

    try {
      const response = await chatWithAI(question);

      const assistantMessage: Message = {
        id: Date.now() + 1,
        role: "assistant",
        timestamp: new Date().toLocaleTimeString(),
        content:
          response.answer ||
          "I could not generate a response at this time.",
      };

      setMessages((previous) => [
        ...previous,
        assistantMessage,
      ]);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "AI service is currently unavailable."
      );
    } finally {
      setLoading(false);

      window.setTimeout(() => {
        inputRef.current?.focus();
      }, 100);
    }
  };

  const handleKeyDown = (
    event: KeyboardEvent<HTMLInputElement>
  ) => {
    if (
      event.key === "Enter" &&
      !event.shiftKey
    ) {
      event.preventDefault();
      sendMessage();
    }
  };

  const clearConversation = () => {
    setMessages([
      {
        ...INITIAL_MESSAGE,
        id: Date.now(),
        timestamp: new Date().toLocaleTimeString(),
        content:
          "Conversation cleared. How can I assist you with immigration, visas, document verification, or compliance today?",
      },
    ]);

    setError("");
    setInput("");

    window.setTimeout(() => {
      inputRef.current?.focus();
    }, 100);
  };

  return (
    <div className="min-h-screen">
      <main className="w-full px-4 py-6 sm:px-6 lg:px-8 lg:py-8">

        {/* =========================================================
            PAGE INTRODUCTION
        ========================================================== */}

        <motion.section
          initial={{
            opacity: 0,
            y: 16,
          }}
          animate={{
            opacity: 1,
            y: 0,
          }}
          transition={{
            duration: 0.35,
          }}
          className="mb-8"
        >
          <div
            className="
              relative
              overflow-hidden
              rounded-3xl
              border
              border-[#3C4C61]/30
              bg-gradient-to-br
              from-[#071426]
              via-[#0B1F3A]
              to-[#3C4C61]
              shadow-xl
            "
          >
            {/* Decorative background elements */}

            <div
              className="
                pointer-events-none
                absolute
                -right-24
                -top-28
                h-80
                w-80
                rounded-full
                bg-[#C6A15B]/10
                blur-3xl
              "
            />

            <div
              className="
                pointer-events-none
                absolute
                -bottom-32
                left-1/3
                h-72
                w-72
                rounded-full
                bg-blue-500/10
                blur-3xl
              "
            />

            <div className="relative grid lg:grid-cols-[1fr_280px]">

              {/* Main content */}

              <div className="px-6 py-8 sm:px-8 lg:px-10 lg:py-10">

                <div
                  className="
                    inline-flex
                    items-center
                    gap-2
                    rounded-full
                    border
                    border-[#C6A15B]/25
                    bg-[#C6A15B]/10
                    px-3.5
                    py-1.5
                    text-xs
                    font-bold
                    uppercase
                    tracking-[0.16em]
                    text-[#C6A15B]
                  "
                >
                  <Sparkles size={14} />

                  AI Immigration Intelligence
                </div>

                <h1
                  className="
                    mt-5
                    text-3xl
                    font-bold
                    tracking-tight
                    text-white
                    sm:text-4xl
                    lg:text-[44px]
                    lg:leading-tight
                  "
                >
                  MukondoGTech AI Assistant
                </h1>

                <p
                  className="
                    mt-4
                    max-w-3xl
                    text-sm
                    leading-7
                    text-slate-300
                    sm:text-base
                  "
                >
                  Get practical guidance on visas, residency,
                  immigration documents, compliance requirements,
                  travel permits, and related immigration processes.
                </p>

                {/* Capability badges */}

                <div className="mt-7 flex flex-wrap gap-2.5">
                  <StatusBadge
                    icon={<Globe size={14} />}
                    text="Global Guidance"
                  />

                  <StatusBadge
                    icon={<FileSearch size={14} />}
                    text="Document Intelligence"
                  />

                  <StatusBadge
                    icon={<ShieldCheck size={14} />}
                    text="Secure Processing"
                  />
                </div>
              </div>

              {/* AI status */}

              <div
                className="
                  border-t
                  border-white/10
                  bg-white/[0.035]
                  px-6
                  py-6
                  backdrop-blur-sm
                  lg:border-l
                  lg:border-t-0
                  lg:px-7
                  lg:py-8
                "
              >
                <div className="flex h-full flex-col justify-center">

                  <div className="flex items-center gap-3">
                    <div
                      className="
                        flex
                        h-12
                        w-12
                        shrink-0
                        items-center
                        justify-center
                        rounded-2xl
                        border
                        border-emerald-400/20
                        bg-emerald-400/10
                        text-emerald-400
                      "
                    >
                      <Bot size={23} />
                    </div>

                    <div>
                      <p className="text-sm font-semibold text-white">
                        AI Assistant
                      </p>

                      <div className="mt-1 flex items-center gap-2">
                        <span className="relative flex h-2.5 w-2.5">
                          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-60" />

                          <span className="relative inline-flex h-2.5 w-2.5 rounded-full bg-emerald-400" />
                        </span>

                        <span className="text-xs text-slate-400">
                          Online and ready
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="mt-6 border-t border-white/10 pt-5">
                    <p className="text-xs leading-5 text-slate-400">
                      Ask questions about immigration requirements,
                      document preparation, visas, permits and
                      compliance.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </motion.section>

        {/* =========================================================
            SUMMARY CARDS
        ========================================================== */}

        <motion.section
          initial={{
            opacity: 0,
            y: 12,
          }}
          animate={{
            opacity: 1,
            y: 0,
          }}
          transition={{
            duration: 0.3,
            delay: 0.05,
          }}
          className="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-3"
        >
          <StatCard
            icon={<MessageSquare size={20} />}
            value={totalMessages}
            label="Messages"
            description="Messages in this conversation"
          />

          <StatCard
            icon={<Bot size={20} />}
            value="24/7"
            label="AI Availability"
            description="Assistant available when you need it"
          />

          <StatCard
            icon={<ShieldCheck size={20} />}
            value="Secure"
            label="Processing"
            description="Protected AI request processing"
          />
        </motion.section>

        {/* =========================================================
            CHAT APPLICATION
        ========================================================== */}

        <motion.section
          initial={{
            opacity: 0,
            y: 12,
          }}
          animate={{
            opacity: 1,
            y: 0,
          }}
          transition={{
            duration: 0.3,
            delay: 0.1,
          }}
          className="
            overflow-hidden
            rounded-3xl
            border
            border-white/10
            bg-white/5
            backdrop-blur-xl
            shadow-xl
          "
        >

          {/* =====================================================
              CHAT HEADER
          ====================================================== */}

          <header
            className="
              flex
              flex-col
              gap-5
              border-b
              border-white/10
              px-5
              py-5
              sm:px-6
              md:flex-row
              md:items-center
              md:justify-between
            "
          >
            <div className="flex items-center gap-4">
              <div
                className="
                  flex
                  h-12
                  w-12
                  shrink-0
                  items-center
                  justify-center
                  rounded-2xl
                  bg-gradient-to-br
                  from-[#071426]
                  to-[#3C4C61]
                  text-[#C6A15B]
                  shadow-md
                "
              >
                <Bot size={23} />
              </div>

              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="text-lg font-bold text-white">
                    Immigration AI Assistant
                  </h2>

                  <span
                    className="
                      inline-flex
                      items-center
                      gap-1.5
                      rounded-full
                      border
                      border-emerald-500/30
                      bg-emerald-500/10
                      px-2
                      py-0.5
                      text-[10px]
                      font-bold
                      uppercase
                      tracking-wide
                      text-emerald-300
                    "
                  >
                    <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                    Online
                  </span>
                </div>

                <p className="mt-1 text-sm text-slate-400">
                  Ask a question to receive immigration guidance.
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={clearConversation}
              disabled={loading}
              className="
                inline-flex
                h-10
                items-center
                justify-center
                gap-2
                rounded-xl
                border
                border-white/15
                bg-white/5
                px-4
                text-sm
                font-semibold
                text-slate-300
                shadow-sm
                transition
                duration-200
                hover:border-red-500/30
                hover:bg-red-500/10
                hover:text-red-300
                focus:outline-none
                focus:ring-2
                focus:ring-red-500/20
                disabled:cursor-not-allowed
                disabled:opacity-50
              "
            >
              <Trash2 size={16} />
              Clear conversation
            </button>
          </header>

          {/* =====================================================
              SUGGESTED QUESTIONS
          ====================================================== */}

          {messages.length <= 1 && (
            <section
              className="
                border-b
                border-white/10
                bg-white/[0.03]
                px-5
                py-6
                sm:px-6
              "
            >
              <div className="mb-4">
                <div className="flex items-center gap-2">
                  <div
                    className="
                      flex
                      h-8
                      w-8
                      items-center
                      justify-center
                      rounded-lg
                      bg-[#071426]
                      text-[#C6A15B]
                    "
                  >
                    <Sparkles size={15} />
                  </div>

                  <div>
                    <p className="text-sm font-semibold text-slate-200">
                      Start with a question
                    </p>

                    <p className="text-xs text-slate-400">
                      Choose a common immigration question below.
                    </p>
                  </div>
                </div>
              </div>

              <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
                {SUGGESTED_PROMPTS.map((prompt) => (
                  <button
                    key={prompt}
                    type="button"
                    onClick={() => sendMessage(prompt)}
                    disabled={loading}
                    className="
                      group
                      flex
                      min-h-[54px]
                      items-center
                      justify-between
                      rounded-xl
                      border
                      border-white/10
                      bg-white/5
                      px-4
                      py-3
                      text-left
                      text-sm
                      font-medium
                      text-slate-300
                      shadow-sm
                      transition
                      duration-200
                      hover:-translate-y-0.5
                      hover:border-[#3C4C61]/40
                      hover:bg-[#071426]
                      hover:text-white
                      hover:shadow-md
                      disabled:cursor-not-allowed
                      disabled:opacity-50
                    "
                  >
                    <span>{prompt}</span>

                    <span
                      className="
                        ml-4
                        shrink-0
                        text-lg
                        text-slate-500
                        transition
                        group-hover:translate-x-1
                        group-hover:text-[#C6A15B]
                      "
                    >
                      →
                    </span>
                  </button>
                ))}
              </div>
            </section>
          )}

          {/* =====================================================
              MESSAGE AREA
          ====================================================== */}

          <div
            className="
              h-[560px]
              overflow-y-auto
              bg-transparent
              px-4
              py-7
              sm:px-6
              sm:py-8
            "
          >
            <div className="mx-auto max-w-4xl space-y-7">

              <AnimatePresence initial={false}>
                {messages.map((message) => (
                  <motion.div
                    key={message.id}
                    initial={{
                      opacity: 0,
                      y: 10,
                    }}
                    animate={{
                      opacity: 1,
                      y: 0,
                    }}
                    transition={{
                      duration: 0.2,
                    }}
                    className={`flex gap-3 sm:gap-4 ${
                      message.role === "user"
                        ? "justify-end"
                        : "justify-start"
                    }`}
                  >

                    {/* Assistant avatar */}

                    {message.role === "assistant" && (
                      <div
                        className="
                          flex
                          h-10
                          w-10
                          shrink-0
                          items-center
                          justify-center
                          rounded-xl
                          bg-gradient-to-br
                          from-[#071426]
                          to-[#3C4C61]
                          text-[#C6A15B]
                          shadow-sm
                          sm:h-11
                          sm:w-11
                        "
                      >
                        <Bot size={18} />
                      </div>
                    )}

                    {/* Message body */}

                    <div
                      className="
                        max-w-[88%]
                        sm:max-w-[76%]
                      "
                    >
                      <div
                        className={`
                          rounded-2xl
                          px-4
                          py-3.5
                          text-sm
                          leading-7
                          shadow-sm
                          sm:px-5
                          sm:py-4
                          ${
                            message.role === "user"
                              ? `
                                rounded-br-md
                                bg-gradient-to-br
                                from-[#071426]
                                to-[#3C4C61]
                                text-white
                              `
                              : `
                                rounded-bl-md
                                border
                                border-white/10
                                bg-white/5
                                text-slate-200
                              `
                          }
                        `}
                      >
                        {message.content}
                      </div>

                      <div
                        className={`
                          mt-2
                          flex
                          items-center
                          gap-1.5
                          text-[11px]
                          text-slate-400
                          ${
                            message.role === "user"
                              ? "justify-end"
                              : "justify-start"
                          }
                        `}
                      >
                        <Clock3 size={11} />
                        {message.timestamp}
                      </div>
                    </div>

                    {/* User avatar */}

                    {message.role === "user" && (
                      <div
                        className="
                          flex
                          h-10
                          w-10
                          shrink-0
                          items-center
                          justify-center
                          rounded-xl
                          bg-gradient-to-br
                          from-[#C6A15B]
                          to-[#F7C94A]
                          text-[#071426]
                          shadow-sm
                          sm:h-11
                          sm:w-11
                        "
                      >
                        <User size={18} />
                      </div>
                    )}
                  </motion.div>
                ))}
              </AnimatePresence>

              {/* AI typing indicator */}

              {loading && (
                <motion.div
                  initial={{
                    opacity: 0,
                    y: 8,
                  }}
                  animate={{
                    opacity: 1,
                    y: 0,
                  }}
                  className="flex gap-3 sm:gap-4"
                >
                  <div
                    className="
                      flex
                      h-10
                      w-10
                      shrink-0
                      items-center
                      justify-center
                      rounded-xl
                      bg-gradient-to-br
                      from-[#071426]
                      to-[#3C4C61]
                      text-[#C6A15B]
                      shadow-sm
                      sm:h-11
                      sm:w-11
                    "
                  >
                    <Bot size={18} />
                  </div>

                  <div
                    className="
                      flex
                      items-center
                      gap-3
                      rounded-2xl
                      rounded-bl-md
                      border
                      border-white/10
                      bg-white/5
                      px-4
                      py-3.5
                      text-sm
                      text-slate-300
                      shadow-sm
                      sm:px-5
                    "
                  >
                    <Loader2
                      size={17}
                      className="
                        animate-spin
                        text-blue-300
                      "
                    />

                    <span>
                      MukondoGTech AI is analysing your request...
                    </span>
                  </div>
                </motion.div>
              )}

              <div ref={bottomRef} />
            </div>
          </div>

          {/* =====================================================
              ERROR MESSAGE
          ====================================================== */}

          <AnimatePresence>
            {error && (
              <motion.div
                initial={{
                  opacity: 0,
                  height: 0,
                }}
                animate={{
                  opacity: 1,
                  height: "auto",
                }}
                exit={{
                  opacity: 0,
                  height: 0,
                }}
                className="
                  border-t
                  border-red-500/30
                  bg-red-500/10
                  px-5
                  py-3.5
                  sm:px-6
                "
              >
                <div className="flex items-start gap-3">
                  <div
                    className="
                      mt-0.5
                      flex
                      h-6
                      w-6
                      shrink-0
                      items-center
                      justify-center
                      rounded-full
                      bg-red-500/15
                      text-red-300
                    "
                  >
                    !
                  </div>

                  <div className="text-sm leading-6 text-red-300">
                    <span className="font-semibold">
                      Unable to process request.
                    </span>{" "}
                    {error}
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          {/* =====================================================
              INPUT AREA
          ====================================================== */}

          <footer
            className="
              border-t
              border-white/10
              bg-transparent
              px-4
              py-5
              sm:px-6
            "
          >
            <div className="mx-auto max-w-4xl">

              <div className="flex gap-2 sm:gap-3">
                <div
                  className="
                    group
                    relative
                    flex-1
                    rounded-2xl
                    bg-gradient-to-r
                    from-[#2563EB]/50
                    via-[#7C5CFF]/40
                    to-[#C6A15B]/50
                    p-[1.5px]
                    shadow-[0_8px_30px_-8px_rgba(37,99,235,0.35)]
                    transition
                    duration-300
                    focus-within:shadow-[0_8px_36px_-6px_rgba(37,99,235,0.55)]
                  "
                >
                  <div
                    className="
                      relative
                      flex
                      items-center
                      rounded-[15px]
                      bg-[#0B1F3A]/90
                      backdrop-blur-xl
                    "
                  >
                    <input
                      ref={inputRef}
                      type="text"
                      value={input}
                      onChange={(event) =>
                        setInput(event.target.value)
                      }
                      onKeyDown={handleKeyDown}
                      disabled={loading}
                      placeholder="Ask about visas, permits, residency, compliance..."
                      className="
                        h-12
                        w-full
                        rounded-[15px]
                        bg-transparent
                        px-4
                        text-sm
                        text-white
                        outline-none
                        transition
                        duration-200
                        placeholder:text-slate-400
                        disabled:cursor-not-allowed
                      "
                    />
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => sendMessage()}
                  disabled={
                    loading ||
                    !input.trim()
                  }
                  aria-label="Send message"
                  className="
                    flex
                    h-12
                    w-12
                    shrink-0
                    items-center
                    justify-center
                    rounded-xl
                    bg-gradient-to-br
                    from-[#2563EB]
                    via-[#7C5CFF]
                    to-[#C6A15B]
                    text-white
                    shadow-md
                    transition
                    duration-200
                    hover:-translate-y-0.5
                    hover:shadow-lg
                    focus:outline-none
                    focus:ring-4
                    focus:ring-[#2563EB]/20
                    disabled:cursor-not-allowed
                    disabled:bg-slate-300
                    disabled:bg-none
                    disabled:text-white
                    disabled:shadow-none
                  "
                >
                  {loading ? (
                    <Loader2
                      size={18}
                      className="animate-spin"
                    />
                  ) : (
                    <Send size={18} />
                  )}
                </button>
              </div>

              {/* Security notice */}

              <div
                className="
                  mt-4
                  flex
                  items-start
                  gap-2.5
                  rounded-xl
                  border
                  border-white/10
                  bg-white/5
                  px-3.5
                  py-3
                  text-[11px]
                  leading-5
                  text-slate-400
                "
              >
                <ShieldCheck
                  size={15}
                  className="
                    mt-0.5
                    shrink-0
                    text-blue-300
                  "
                />

                <span>
                  AI-generated guidance is provided for informational
                  purposes. Always verify immigration requirements
                  with the relevant government authority before making
                  important decisions.
                </span>
              </div>
            </div>
          </footer>
        </motion.section>

        {/* =========================================================
            PAGE FOOTER
        ========================================================== */}

        <motion.div
          initial={{
            opacity: 0,
          }}
          animate={{
            opacity: 1,
          }}
          transition={{
            delay: 0.2,
          }}
          className="
            mt-5
            flex
            flex-col
            gap-3
            rounded-2xl
            border
            border-white/10
            bg-white/5
            px-4
            py-4
            text-xs
            text-slate-400
            shadow-sm
            sm:flex-row
            sm:items-center
            sm:justify-between
          "
        >
          <div className="flex items-center gap-2">
            <CheckCircle2
              size={14}
              className="text-blue-300"
            />

            <span className="font-medium text-slate-300">
              MukondoGTech AI Immigration Intelligence
            </span>
          </div>

          <span>
            Information should be independently verified before use.
          </span>
        </motion.div>
      </main>
    </div>
  );
}

/* ================================================================
   STAT CARD
================================================================ */

function StatCard({
  icon,
  value,
  label,
  description,
}: {
  icon: React.ReactNode;
  value: string | number;
  label: string;
  description: string;
}) {
  return (
    <motion.div
      whileHover={{
        y: -2,
      }}
      transition={{
        duration: 0.2,
      }}
      className="
        group
        rounded-2xl
        border
        border-white/10
        bg-white/5
        p-5
        shadow-sm
        transition-shadow
        duration-200
        hover:shadow-md
      "
    >
      <div className="flex items-start justify-between gap-4">
        <div
          className="
            flex
            h-11
            w-11
            shrink-0
            items-center
            justify-center
            rounded-xl
            bg-gradient-to-br
            from-[#071426]
            to-[#3C4C61]
            text-[#C6A15B]
            shadow-sm
          "
        >
          {icon}
        </div>

        <span
          className="
            rounded-full
            border
            border-white/10
            bg-white/5
            px-2.5
            py-1
            text-[10px]
            font-bold
            uppercase
            tracking-wide
            text-slate-400
          "
        >
          {label}
        </span>
      </div>

      <div className="mt-5">
        <div
          className="
            text-2xl
            font-bold
            tracking-tight
            text-white
          "
        >
          {value}
        </div>

        <p className="mt-1.5 text-xs leading-5 text-slate-500">
          {description}
        </p>
      </div>
    </motion.div>
  );
}

/* ================================================================
   STATUS BADGE
================================================================ */

function StatusBadge({
  icon,
  text,
}: {
  icon: React.ReactNode;
  text: string;
}) {
  return (
    <div
      className="
        inline-flex
        items-center
        gap-2
        rounded-full
        border
        border-white/10
        bg-white/[0.07]
        px-3.5
        py-2
        text-xs
        font-medium
        text-slate-200
        backdrop-blur-sm
      "
    >
      <span className="text-[#C6A15B]">
        {icon}
      </span>

      <span>{text}</span>
    </div>
  );
}


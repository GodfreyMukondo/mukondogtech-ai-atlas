
import {
  useState,
  type KeyboardEvent,
} from "react";

import { AnimatePresence, motion } from "framer-motion";
import type { Variants } from "framer-motion";

import {
  ArrowUpRight,
  Bot,
  CheckCircle2,
  Clock3,
  FileQuestion,
  Loader2,
  MessageCircle,
  Send,
  ShieldCheck,
  Sparkles,
  Zap,
} from "lucide-react";

import { askQuestion } from "../api/chatApi";

/*
|--------------------------------------------------------------------------
| CONSTANTS
|--------------------------------------------------------------------------
*/

const exampleQuestions = [
  {
    question:
      "What documents are required for a work permit application?",
    icon: FileQuestion,
  },
  {
    question:
      "Can I apply for permanent residency after a student visa?",
    icon: CheckCircle2,
  },
  {
    question:
      "How do I verify whether my documents are complete?",
    icon: ShieldCheck,
  },
  {
    question:
      "What are the common reasons visa applications are rejected?",
    icon: Clock3,
  },
];

/*
|--------------------------------------------------------------------------
| ANIMATION CONFIGURATION
|--------------------------------------------------------------------------
*/

const containerVariants: Variants = {
  hidden: {
    opacity: 0,
  },

  visible: {
    opacity: 1,

    transition: {
      staggerChildren: 0.08,
    },
  },
};

const itemVariants: Variants = {
  hidden: {
    opacity: 0,
    y: 24,
  },

  visible: {
    opacity: 1,
    y: 0,

    transition: {
      duration: 0.55,
      ease: "easeOut",
    },
  },
};

/*
|--------------------------------------------------------------------------
| CHAT PAGE
|--------------------------------------------------------------------------
*/

export default function ChatPage() {
  const [question, setQuestion] = useState("");
  const [answer, setAnswer] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  /*
  |--------------------------------------------------------------------------
  | ASK AI
  |--------------------------------------------------------------------------
  */

  const handleAsk = async () => {
    const prompt = question.trim();

    if (!prompt || loading) {
      return;
    }

    setLoading(true);
    setError("");
    setAnswer("");

    try {
      const response = await askQuestion(prompt);

      setAnswer(
        response.answer ||
          "No response was returned by the AI service.",
      );
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to contact the AI service.",
      );
    } finally {
      setLoading(false);
    }
  };

  /*
  |--------------------------------------------------------------------------
  | KEYBOARD HANDLER
  |--------------------------------------------------------------------------
  */

  const handleKeyDown = (
    event: KeyboardEvent<HTMLTextAreaElement>,
  ) => {
    if (
      event.key === "Enter" &&
      event.ctrlKey
    ) {
      event.preventDefault();

      void handleAsk();
    }
  };

  /*
  |--------------------------------------------------------------------------
  | EXAMPLE QUESTION
  |--------------------------------------------------------------------------
  */

  const selectExample = (text: string) => {
    setQuestion(text);
    setError("");

    requestAnimationFrame(() => {
      document
        .getElementById("question")
        ?.focus();
    });
  };

  /*
  |--------------------------------------------------------------------------
  | RENDER
  |--------------------------------------------------------------------------
  */

  return (
    <main
      className="
        min-h-screen
        overflow-hidden
        text-slate-100
      "
    >
      {/* =========================================================
          HERO
          ========================================================= */}

      <section
        className="
          relative
          isolate
          overflow-hidden
          bg-gradient-to-br
          from-[#071426]
          via-[#0B1F3A]
          to-[#3C4C61]
          text-white
        "
      >
        {/* Atmospheric yellow glow */}

        <motion.div
          aria-hidden="true"
          className="
            pointer-events-none
            absolute
            -right-32
            -top-32
            h-[28rem]
            w-[28rem]
            rounded-full
            bg-[#C6A15B]/10
            blur-[120px]
          "
          animate={{
            scale: [1, 1.12, 1],
            opacity: [0.45, 0.7, 0.45],
          }}
          transition={{
            duration: 5,
            repeat: Infinity,
            ease: "easeInOut",
          }}
        />

        {/* Soft yellow glow */}

        <motion.div
          aria-hidden="true"
          className="
            pointer-events-none
            absolute
            -bottom-40
            left-[-10rem]
            h-[24rem]
            w-[24rem]
            rounded-full
            bg-[#D4B984]/10
            blur-[110px]
          "
          animate={{
            x: [0, 40, 0],
            y: [0, -20, 0],
          }}
          transition={{
            duration: 7,
            repeat: Infinity,
            ease: "easeInOut",
          }}
        />

        {/* Grid */}

        <div
          aria-hidden="true"
          className="
            pointer-events-none
            absolute
            inset-0
            opacity-[0.035]
            [background-image:linear-gradient(#ffffff_1px,transparent_1px),linear-gradient(to_right,#ffffff_1px,transparent_1px)]
            [background-size:48px_48px]
          "
        />

        <div
          className="
            relative
            mx-auto
            max-w-7xl
            px-6
            py-20
            sm:py-24
            lg:px-8
            lg:py-28
          "
        >
          <motion.div
            variants={containerVariants}
            initial="hidden"
            animate="visible"
            className="max-w-4xl"
          >
            {/* Platform badge */}

            <motion.div variants={itemVariants}>
              <div
                className="
                  inline-flex
                  items-center
                  gap-2.5
                  rounded-full
                  border
                  border-[#C6A15B]/30
                  bg-white/[0.06]
                  px-4
                  py-2
                  text-sm
                  font-semibold
                  text-[#D4B984]
                  shadow-[0_8px_30px_rgba(198, 161, 91,0.08)]
                  backdrop-blur-xl
                "
              >
                <motion.span
                  animate={{
                    rotate: [0, 8, -8, 0],
                  }}
                  transition={{
                    duration: 2.5,
                    repeat: Infinity,
                    ease: "easeInOut",
                  }}
                >
                  <Sparkles
                    size={16}
                    aria-hidden="true"
                  />
                </motion.span>

                MukondoGTech AI Assistant
              </div>
            </motion.div>

            {/* Heading */}

            <motion.h1
              variants={itemVariants}
              className="
                mt-7
                max-w-4xl
                text-4xl
                font-black
                leading-[1.05]
                tracking-tight
                sm:text-5xl
                md:text-6xl
                lg:text-7xl
              "
            >
              Immigration Guidance

              <span
                className="
                  mt-2
                  block
                  bg-gradient-to-r
                  from-[#C6A15B]
                  via-[#D4B984]
                  to-[#C6A15B]
                  bg-[length:200%_auto]
                  bg-clip-text
                  text-transparent
                "
              >
                Powered by AI
              </span>
            </motion.h1>

            {/* Description */}

            <motion.p
              variants={itemVariants}
              className="
                mt-7
                max-w-3xl
                text-base
                leading-8
                text-slate-300
                sm:text-lg
              "
            >
              Ask questions about visas, residency,
              permits, immigration requirements,
              compliance obligations, and document
              preparation. Receive structured
              AI-assisted guidance in seconds.
            </motion.p>

            {/* Status indicators */}

            <motion.div
              variants={itemVariants}
              className="
                mt-8
                flex
                flex-wrap
                items-center
                gap-x-6
                gap-y-3
                text-sm
                text-slate-300
              "
            >
              <span className="flex items-center gap-2">
                <motion.span
                  className="
                    h-2
                    w-2
                    rounded-full
                    bg-emerald-400
                  "
                  animate={{
                    scale: [1, 1.35, 1],
                    opacity: [0.7, 1, 0.7],
                  }}
                  transition={{
                    duration: 1.5,
                    repeat: Infinity,
                  }}
                />

                AI Service Available
              </span>

              <span className="flex items-center gap-2">
                <ShieldCheck
                  size={16}
                  className="text-[#D4B984]"
                  aria-hidden="true"
                />

                Privacy-conscious processing
              </span>

              <span className="flex items-center gap-2">
                <Zap
                  size={16}
                  className="text-[#C6A15B]"
                  aria-hidden="true"
                />

                Fast AI responses
              </span>
            </motion.div>
          </motion.div>
        </div>
      </section>

      {/* =========================================================
          MAIN CONTENT
          ========================================================= */}

      <section
        className="
          relative
          mx-auto
          max-w-7xl
          px-6
          py-12
          sm:py-16
          lg:px-8
          lg:py-20
        "
      >
        <div
          className="
            grid
            gap-8
            lg:grid-cols-[320px_minmax(0,1fr)]
          "
        >
          {/* =====================================================
              SIDEBAR
              ===================================================== */}

          <motion.aside
            initial={{
              opacity: 0,
              x: -25,
            }}
            whileInView={{
              opacity: 1,
              x: 0,
            }}
            viewport={{
              once: true,
              amount: 0.15,
            }}
            transition={{
              duration: 0.6,
            }}
            className="
              h-fit
              rounded-[1.75rem]
              border
              border-slate-200
              bg-white
              p-5
              shadow-[0_15px_50px_rgba(7, 20, 38,0.07)]
              lg:sticky
              lg:top-24
            "
          >
            <div className="flex items-start gap-3">
              <div
                className="
                  flex
                  h-11
                  w-11
                  shrink-0
                  items-center
                  justify-center
                  rounded-2xl
                  bg-gradient-to-br
                  from-[#C6A15B]
                  to-[#D4B984]
                  text-[#071426]
                  shadow-[0_8px_25px_rgba(198, 161, 91,0.22)]
                "
              >
                <MessageCircle
                  size={21}
                  aria-hidden="true"
                />
              </div>

              <div>
                <h2
                  className="
                    text-lg
                    font-extrabold
                    text-[#071426]
                  "
                >
                  Example Questions
                </h2>

                <p
                  className="
                    mt-1
                    text-sm
                    leading-6
                    text-slate-500
                  "
                >
                  Start with a common immigration
                  question.
                </p>
              </div>
            </div>

            {/* Example questions */}

            <div className="mt-6 space-y-2.5">
              {exampleQuestions.map(
                (item, index) => {
                  const Icon = item.icon;

                  return (
                    <motion.button
                      key={item.question}
                      type="button"
                      onClick={() =>
                        selectExample(
                          item.question,
                        )
                      }
                      initial={{
                        opacity: 0,
                        y: 12,
                      }}
                      whileInView={{
                        opacity: 1,
                        y: 0,
                      }}
                      viewport={{
                        once: true,
                      }}
                      transition={{
                        delay: index * 0.07,
                      }}
                      whileHover={{
                        x: 4,
                      }}
                      whileTap={{
                        scale: 0.98,
                      }}
                      className="
                        group
                        flex
                        w-full
                        items-start
                        gap-3
                        rounded-2xl
                        border
                        border-slate-200
                        bg-[#F8F6F1]
                        p-3.5
                        text-left
                        transition-colors
                        hover:border-[#C6A15B]/60
                        hover:bg-[#FFF8E1]
                        focus:outline-none
                        focus:ring-2
                        focus:ring-[#C6A15B]/30
                      "
                    >
                      <span
                        className="
                          mt-0.5
                          flex
                          h-8
                          w-8
                          shrink-0
                          items-center
                          justify-center
                          rounded-xl
                          bg-white
                          text-[#071426]
                          shadow-sm
                          transition
                          group-hover:bg-[#D4B984]
                        "
                      >
                        <Icon
                          size={15}
                          aria-hidden="true"
                        />
                      </span>

                      <span
                        className="
                          text-sm
                          font-medium
                          leading-5
                          text-slate-700
                        "
                      >
                        {item.question}
                      </span>
                    </motion.button>
                  );
                },
              )}
            </div>

            {/* Security notice */}

            <div
              className="
                relative
                mt-7
                overflow-hidden
                rounded-2xl
                border
                border-[#C6A15B]/20
                bg-gradient-to-br
                from-[#FFF8E1]
                to-[#FFFDF5]
                p-4
              "
            >
              <motion.div
                aria-hidden="true"
                className="
                  absolute
                  -right-8
                  -top-8
                  h-24
                  w-24
                  rounded-full
                  bg-[#D4B984]/20
                  blur-2xl
                "
                animate={{
                  scale: [1, 1.15, 1],
                }}
                transition={{
                  duration: 3,
                  repeat: Infinity,
                }}
              />

              <div className="relative flex gap-3">
                <ShieldCheck
                  size={19}
                  className="
                    mt-0.5
                    shrink-0
                    text-[#D79A00]
                  "
                  aria-hidden="true"
                />

                <div>
                  <p
                    className="
                      font-bold
                      text-[#071426]
                    "
                  >
                    Secure Processing
                  </p>

                  <p
                    className="
                      mt-1
                      text-xs
                      leading-5
                      text-slate-600
                    "
                  >
                    Requests are processed through
                    secure AI services and protected
                    infrastructure.
                  </p>
                </div>
              </div>
            </div>
          </motion.aside>

          {/* =====================================================
              CHAT PANEL
              ===================================================== */}

          <motion.section
            initial={{
              opacity: 0,
              y: 25,
            }}
            whileInView={{
              opacity: 1,
              y: 0,
            }}
            viewport={{
              once: true,
              amount: 0.1,
            }}
            transition={{
              duration: 0.6,
            }}
            aria-labelledby="question-heading"
            className="
              relative
              overflow-hidden
              rounded-[1.75rem]
              border
              border-white/10
              bg-white/5
              backdrop-blur-xl
              shadow-[0_15px_60px_rgba(7, 20, 38,0.25)]
            "
          >
            {/* Top accent */}

            <div
              className="
                h-1
                w-full
                bg-gradient-to-r
                from-[#C6A15B]
                via-[#D4B984]
                to-[#C6A15B]
              "
            />

            <div className="p-6 sm:p-8 lg:p-10">
              {/* Header */}

              <div className="flex items-start justify-between gap-5">
                <div>
                  <div
                    className="
                      flex
                      items-center
                      gap-3
                    "
                  >
                    <motion.div
                      animate={{
                        rotate: [0, 2, -2, 0],
                      }}
                      transition={{
                        duration: 3,
                        repeat: Infinity,
                        ease: "easeInOut",
                      }}
                      className="
                        flex
                        h-11
                        w-11
                        items-center
                        justify-center
                        rounded-2xl
                        bg-[#071426]
                        text-[#D4B984]
                      "
                    >
                      <Bot
                        size={22}
                        aria-hidden="true"
                      />
                    </motion.div>

                    <div>
                      <h2
                        id="question-heading"
                        className="
                          text-xl
                          font-extrabold
                          text-white
                        "
                      >
                        Ask Your Question
                      </h2>

                      <div
                        className="
                          mt-1
                          flex
                          items-center
                          gap-2
                          text-xs
                          text-slate-400
                        "
                      >
                        <span
                          className="
                            h-1.5
                            w-1.5
                            rounded-full
                            bg-emerald-500
                          "
                        />

                        AI assistant online
                      </div>
                    </div>
                  </div>
                </div>

                <div
                  className="
                    hidden
                    items-center
                    gap-2
                    rounded-full
                    border
                    border-[#C6A15B]/30
                    bg-[#C6A15B]/10
                    px-3
                    py-1.5
                    text-xs
                    font-semibold
                    text-[#C6A15B]
                    sm:flex
                  "
                >
                  <Sparkles
                    size={13}
                    aria-hidden="true"
                  />

                  AI Powered
                </div>
              </div>

              <p
                className="
                  mt-7
                  max-w-2xl
                  text-sm
                  leading-6
                  text-slate-400
                "
              >
                Describe your immigration question
                clearly. The AI assistant will provide
                structured guidance based on the
                information available to it.
              </p>

              {/* Textarea */}

              <div
                className="
                  group
                  relative
                  mt-5
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
                <textarea
                  id="question"
                  rows={7}
                  value={question}
                  onChange={(event) =>
                    setQuestion(
                      event.target.value,
                    )
                  }
                  onKeyDown={handleKeyDown}
                  disabled={loading}
                  placeholder="Describe your immigration question in as much detail as possible..."
                  aria-label="Immigration question"
                  className="
                    w-full
                    resize-none
                    rounded-[15px]
                    bg-[#0B1F3A]/90
                    backdrop-blur-xl
                    p-5
                    text-sm
                    leading-7
                    text-white
                    outline-none
                    transition-all
                    placeholder:text-slate-400
                    disabled:cursor-not-allowed
                    disabled:opacity-70
                  "
                />

                <div
                  className="
                    pointer-events-none
                    absolute
                    bottom-3
                    right-4
                    text-[11px]
                    text-slate-500
                  "
                >
                  Ctrl + Enter
                </div>
              </div>

              {/* Submit area */}

              <div
                className="
                  mt-5
                  flex
                  flex-col
                  gap-4
                  sm:flex-row
                  sm:items-center
                  sm:justify-between
                "
              >
                <p
                  className="
                    text-xs
                    leading-5
                    text-slate-400
                  "
                >
                  AI-generated guidance should be
                  independently verified for official
                  immigration decisions.
                </p>

                <motion.button
                  type="button"
                  onClick={() => void handleAsk()}
                  disabled={
                    loading ||
                    !question.trim()
                  }
                  whileHover={
                    !loading && question.trim()
                      ? {
                          y: -2,
                        }
                      : undefined
                  }
                  whileTap={
                    !loading && question.trim()
                      ? {
                          scale: 0.97,
                        }
                      : undefined
                  }
                  className="
                    inline-flex
                    min-h-12
                    shrink-0
                    items-center
                    justify-center
                    gap-2.5
                    rounded-2xl
                    bg-gradient-to-r
                    from-[#C6A15B]
                    to-[#D4B984]
                    px-6
                    py-3
                    font-bold
                    text-[#071426]
                    shadow-[0_10px_30px_rgba(198, 161, 91,0.22)]
                    transition-all
                    hover:shadow-[0_15px_40px_rgba(198, 161, 91,0.32)]
                    focus:outline-none
                    focus:ring-2
                    focus:ring-[#C6A15B]/40
                    disabled:cursor-not-allowed
                    disabled:opacity-50
                    disabled:shadow-none
                  "
                >
                  {loading ? (
                    <>
                      <Loader2
                        size={18}
                        className="animate-spin"
                        aria-hidden="true"
                      />

                      Generating Response
                    </>
                  ) : (
                    <>
                      <Send
                        size={18}
                        aria-hidden="true"
                      />

                      Ask AI Assistant

                      <ArrowUpRight
                        size={17}
                        aria-hidden="true"
                      />
                    </>
                  )}
                </motion.button>
              </div>

              {/* Error */}

              <AnimatePresence>
                {error && (
                  <motion.div
                    initial={{
                      opacity: 0,
                      y: 10,
                    }}
                    animate={{
                      opacity: 1,
                      y: 0,
                    }}
                    exit={{
                      opacity: 0,
                      y: -10,
                    }}
                    role="alert"
                    className="
                      mt-6
                      rounded-2xl
                      border
                      border-red-500/30
                      bg-red-500/10
                      p-4
                      text-sm
                      leading-6
                      text-red-300
                    "
                  >
                    {error}
                  </motion.div>
                )}
              </AnimatePresence>

              {/* =================================================
                  AI RESPONSE
                  ================================================= */}

              <AnimatePresence mode="wait">
                {loading && (
                  <motion.div
                    key="loading"
                    initial={{
                      opacity: 0,
                      y: 15,
                    }}
                    animate={{
                      opacity: 1,
                      y: 0,
                    }}
                    exit={{
                      opacity: 0,
                    }}
                    className="
                      mt-8
                      rounded-3xl
                      border
                      border-[#C6A15B]/20
                      bg-gradient-to-br
                      from-[#FFFDF7]
                      to-[#F8F6F1]
                      p-6
                    "
                  >
                    <div className="flex items-center gap-4">
                      <div
                        className="
                          relative
                          flex
                          h-12
                          w-12
                          items-center
                          justify-center
                          rounded-2xl
                          bg-[#071426]
                        "
                      >
                        <Bot
                          size={22}
                          className="text-[#D4B984]"
                          aria-hidden="true"
                        />

                        <motion.span
                          className="
                            absolute
                            inset-[-4px]
                            rounded-2xl
                            border
                            border-[#C6A15B]/40
                          "
                          animate={{
                            scale: [1, 1.15, 1],
                            opacity: [0.7, 0.1, 0.7],
                          }}
                          transition={{
                            duration: 1.8,
                            repeat: Infinity,
                          }}
                        />
                      </div>

                      <div>
                        <p
                          className="
                            font-bold
                            text-[#071426]
                          "
                        >
                          AI is analyzing your
                          question
                        </p>

                        <p
                          className="
                            mt-1
                            text-sm
                            text-slate-500
                          "
                        >
                          Preparing a structured
                          response...
                        </p>
                      </div>
                    </div>

                    <div
                      className="
                        mt-6
                        flex
                        gap-2
                      "
                    >
                      {[0, 1, 2].map(
                        (item) => (
                          <motion.span
                            key={item}
                            className="
                              h-2
                              w-2
                              rounded-full
                              bg-[#C6A15B]
                            "
                            animate={{
                              y: [0, -5, 0],
                              opacity: [
                                0.35,
                                1,
                                0.35,
                              ],
                            }}
                            transition={{
                              duration: 0.8,
                              repeat: Infinity,
                              delay:
                                item * 0.15,
                            }}
                          />
                        ),
                      )}
                    </div>
                  </motion.div>
                )}

                {answer && !loading && (
                  <motion.article
                    key="answer"
                    initial={{
                      opacity: 0,
                      y: 20,
                      scale: 0.98,
                    }}
                    animate={{
                      opacity: 1,
                      y: 0,
                      scale: 1,
                    }}
                    transition={{
                      duration: 0.5,
                      ease: "easeOut",
                    }}
                    className="
                      mt-8
                      overflow-hidden
                      rounded-3xl
                      border
                      border-[#C6A15B]/20
                      bg-gradient-to-br
                      from-[#FFFDF8]
                      to-[#F8F6F1]
                    "
                  >
                    {/* Response header */}

                    <div
                      className="
                        border-b
                        border-[#C6A15B]/15
                        px-6
                        py-5
                      "
                    >
                      <div className="flex items-center gap-3">
                        <motion.div
                          animate={{
                            y: [0, -2, 0],
                          }}
                          transition={{
                            duration: 2.5,
                            repeat: Infinity,
                            ease: "easeInOut",
                          }}
                          className="
                            flex
                            h-12
                            w-12
                            items-center
                            justify-center
                            rounded-2xl
                            bg-[#071426]
                            shadow-lg
                          "
                        >
                          <Bot
                            size={22}
                            className="text-[#D4B984]"
                            aria-hidden="true"
                          />
                        </motion.div>

                        <div>
                          <div
                            className="
                              flex
                              items-center
                              gap-2
                            "
                          >
                            <h3
                              className="
                                font-extrabold
                                text-[#071426]
                              "
                            >
                              AI Assistant Response
                            </h3>

                            <span
                              className="
                                rounded-full
                                bg-emerald-100
                                px-2
                                py-0.5
                                text-[10px]
                                font-bold
                                uppercase
                                tracking-wide
                                text-emerald-700
                              "
                            >
                              Ready
                            </span>
                          </div>

                          <p
                            className="
                              mt-1
                              text-xs
                              text-slate-500
                            "
                          >
                            Generated by
                            MukondoGTech AI
                          </p>
                        </div>
                      </div>
                    </div>

                    {/* Response body */}

                    <div
                      className="
                        whitespace-pre-wrap
                        px-6
                        py-7
                        text-sm
                        leading-8
                        text-[#0B1F3A]
                        sm:text-base
                      "
                    >
                      {answer}
                    </div>

                    {/* Response footer */}

                    <div
                      className="
                        flex
                        flex-col
                        gap-3
                        border-t
                        border-[#C6A15B]/10
                        px-6
                        py-4
                        sm:flex-row
                        sm:items-center
                        sm:justify-between
                      "
                    >
                      <div
                        className="
                          flex
                          items-center
                          gap-2
                          text-xs
                          text-slate-500
                        "
                      >
                        <ShieldCheck
                          size={14}
                          className="text-emerald-600"
                          aria-hidden="true"
                        />

                        AI-assisted information
                      </div>

                      <p
                        className="
                          text-xs
                          text-slate-400
                        "
                      >
                        Verify important information
                        with the relevant authority.
                      </p>
                    </div>
                  </motion.article>
                )}
              </AnimatePresence>
            </div>
          </motion.section>
        </div>
      </section>

      {/* =========================================================
          FLOATING AI ASSISTANT
          ========================================================= */}

      <motion.div
        className="
          fixed
          bottom-6
          right-5
          z-[100]
          sm:bottom-8
          sm:right-8
        "
        initial={{
          opacity: 0,
          scale: 0.7,
        }}
        animate={{
          opacity: 1,
          scale: 1,
          y: [0, -6, 0],
        }}
        transition={{
          opacity: {
            duration: 0.5,
          },
          scale: {
            duration: 0.5,
          },
          y: {
            duration: 3,
            repeat: Infinity,
            ease: "easeInOut",
          },
        }}
      >
        {/* Outer live glow */}

        <motion.span
          aria-hidden="true"
          className="
            absolute
            inset-[-9px]
            rounded-full
            bg-[#C6A15B]/20
            blur-md
          "
          animate={{
            scale: [0.9, 1.25, 0.9],
            opacity: [0.45, 0.12, 0.45],
          }}
          transition={{
            duration: 2.2,
            repeat: Infinity,
            ease: "easeInOut",
          }}
        />

        {/* Animated ring */}

        <motion.span
          aria-hidden="true"
          className="
            absolute
            inset-[-5px]
            rounded-full
            border-2
            border-[#D4B984]/50
          "
          animate={{
            scale: [1, 1.14, 1],
            opacity: [0.8, 0.25, 0.8],
          }}
          transition={{
            duration: 2,
            repeat: Infinity,
            ease: "easeInOut",
          }}
        />

        <motion.button
          type="button"
          aria-label="Focus AI assistant question input"
          onClick={() => {
            document
              .getElementById("question")
              ?.focus();
          }}
          whileHover={{
            scale: 1.08,
          }}
          whileTap={{
            scale: 0.93,
          }}
          className="
            group
            relative
            flex
            h-16
            w-16
            items-center
            justify-center
            rounded-full
            border
            border-[#D4B984]/70
            bg-gradient-to-br
            from-[#C6A15B]
            to-[#D4B984]
            text-[#071426]
            shadow-[0_12px_45px_rgba(198, 161, 91,0.35)]
            focus:outline-none
            focus:ring-2
            focus:ring-[#C6A15B]
            focus:ring-offset-4
            focus:ring-offset-[#F8F6F1]
          "
        >
          <motion.span
            animate={{
              rotate: [0, 4, -4, 0],
            }}
            transition={{
              duration: 2.5,
              repeat: Infinity,
              ease: "easeInOut",
            }}
          >
            <Bot
              size={29}
              strokeWidth={2.3}
              aria-hidden="true"
            />
          </motion.span>

          {/* Live status */}

          <motion.span
            aria-hidden="true"
            className="
              absolute
              right-0.5
              top-0.5
              h-4
              w-4
              rounded-full
              border-2
              border-[#071426]
              bg-emerald-400
            "
            animate={{
              scale: [1, 1.2, 1],
            }}
            transition={{
              duration: 1.5,
              repeat: Infinity,
            }}
          />

          {/* Tooltip */}

          <span
            className="
              pointer-events-none
              absolute
              bottom-full
              right-0
              mb-3
              whitespace-nowrap
              rounded-xl
              bg-[#071426]
              px-3
              py-2
              text-xs
              font-semibold
              text-white
              opacity-0
              shadow-xl
              transition-opacity
              duration-200
              group-hover:opacity-100
            "
          >
            Ask MukondoGTech AI
          </span>
        </motion.button>
      </motion.div>
    </main>
  );
}


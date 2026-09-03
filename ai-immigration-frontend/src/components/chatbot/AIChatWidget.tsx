import { useState } from "react";

import {
  AnimatePresence,
  motion,
} from "framer-motion";

import {
  Bot,
  MessageCircle,
  Sparkles,
  X,
} from "lucide-react";

import AIChatWidgetContent from "./AIChatWidgetContent";

/**
 * ============================================================
 * AI CHAT WIDGET
 * ============================================================
 *
 * Global floating AI assistant.
 *
 * Responsibilities:
 * - Floating AI launcher
 * - Live animated status indicator
 * - Gold / blue visual identity
 * - Open / close animation
 * - Responsive chat window
 * - Accessible controls
 * - AIChatWidgetContent integration
 *
 * This component should normally be mounted once from
 * LandingLayout.tsx.
 * ============================================================
 */

export default function AIChatWidget() {
  const [open, setOpen] = useState(false);

  const handleOpen = () => {
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
  };

  return (
    <>
      {/* ======================================================
          FLOATING AI LAUNCHER
      ====================================================== */}

      <AnimatePresence>
        {!open && (
          <motion.div
            initial={{
              opacity: 0,
              scale: 0.6,
              y: 30,
            }}
            animate={{
              opacity: 1,
              scale: 1,
              y: 0,
            }}
            exit={{
              opacity: 0,
              scale: 0.6,
              y: 30,
            }}
            transition={{
              duration: 0.35,
              ease: "easeOut",
            }}
            className="
              fixed
              bottom-6
              right-6
              z-[999]
            "
          >
            {/* --------------------------------------------------
                LIVE OUTER PULSE
            -------------------------------------------------- */}

            <motion.div
              aria-hidden="true"
              animate={{
                scale: [1, 1.18, 1],
                opacity: [0.45, 0, 0.45],
              }}
              transition={{
                duration: 2.2,
                repeat: Infinity,
                ease: "easeInOut",
              }}
              className="
                absolute
                inset-[-10px]
                rounded-full
                bg-gradient-to-r
                from-[#F4B81A]
                via-[#FFD96A]
                to-[#2563EB]
                blur-md
              "
            />

            {/* --------------------------------------------------
                SECOND LIVE RING
            -------------------------------------------------- */}

            <motion.div
              aria-hidden="true"
              animate={{
                scale: [1, 1.12, 1],
                opacity: [0.7, 0.15, 0.7],
              }}
              transition={{
                duration: 1.8,
                repeat: Infinity,
                ease: "easeInOut",
                delay: 0.25,
              }}
              className="
                absolute
                inset-[-5px]
                rounded-full
                border-2
                border-[#F4B81A]/60
              "
            />

            {/* --------------------------------------------------
                FLOATING BUTTON
            -------------------------------------------------- */}

            <motion.button
              type="button"
              onClick={handleOpen}
              aria-label="Open MukondoGTech AI Assistant"
              aria-expanded={open}
              title="Open AI Assistant"
              animate={{
                y: [0, -5, 0],
              }}
              transition={{
                duration: 3,
                repeat: Infinity,
                ease: "easeInOut",
              }}
              whileHover={{
                scale: 1.08,
              }}
              whileTap={{
                scale: 0.94,
              }}
              className="
                group
                relative
                flex
                h-16
                w-16
                items-center
                justify-center
                overflow-hidden
                rounded-full
                border
                border-white/30
                bg-gradient-to-br
                from-[#F4B81A]
                via-[#FFD96A]
                to-[#2563EB]
                text-[#071330]
                shadow-[0_12px_40px_rgba(7,19,48,0.35)]
                outline-none
                transition-shadow
                duration-300
                hover:shadow-[0_16px_50px_rgba(244,184,26,0.45)]
                focus-visible:ring-4
                focus-visible:ring-[#F4B81A]/40
              "
            >
              {/* ------------------------------------------------
                  MOVING SHINE
              ------------------------------------------------ */}

              <motion.span
                aria-hidden="true"
                initial={{
                  x: "-150%",
                }}
                animate={{
                  x: "150%",
                }}
                transition={{
                  duration: 2.4,
                  repeat: Infinity,
                  repeatDelay: 2,
                  ease: "easeInOut",
                }}
                className="
                  absolute
                  inset-y-0
                  w-1/3
                  rotate-12
                  bg-white/30
                  blur-sm
                "
              />

              {/* ------------------------------------------------
                  ICON
              ------------------------------------------------ */}

              <motion.div
                animate={{
                  rotate: [0, -5, 5, 0],
                  scale: [1, 1.08, 1],
                }}
                transition={{
                  duration: 2.5,
                  repeat: Infinity,
                  ease: "easeInOut",
                }}
                className="relative z-10"
              >
                <MessageCircle
                  size={29}
                  strokeWidth={2.4}
                />
              </motion.div>

              {/* ------------------------------------------------
                  ONLINE INDICATOR
              ------------------------------------------------ */}

              <motion.span
                aria-label="AI assistant online"
                animate={{
                  scale: [1, 1.25, 1],
                  opacity: [1, 0.7, 1],
                }}
                transition={{
                  duration: 1.5,
                  repeat: Infinity,
                  ease: "easeInOut",
                }}
                className="
                  absolute
                  bottom-1
                  right-1
                  z-20
                  h-4
                  w-4
                  rounded-full
                  border-[3px]
                  border-white
                  bg-emerald-500
                  shadow-lg
                "
              />
            </motion.button>

            {/* --------------------------------------------------
                LIVE LABEL
            -------------------------------------------------- */}

            <motion.div
              initial={{
                opacity: 0,
                x: 10,
              }}
              animate={{
                opacity: 1,
                x: 0,
              }}
              transition={{
                delay: 0.8,
                duration: 0.4,
              }}
              className="
                absolute
                right-20
                top-1/2
                hidden
                -translate-y-1/2
                items-center
                gap-2
                whitespace-nowrap
                rounded-full
                border
                border-white/20
                bg-[#071330]/95
                px-3
                py-1.5
                text-xs
                font-semibold
                text-white
                shadow-xl
                backdrop-blur-xl
                sm:flex
              "
            >
              <motion.span
                animate={{
                  opacity: [1, 0.35, 1],
                }}
                transition={{
                  duration: 1.4,
                  repeat: Infinity,
                }}
                className="
                  h-2
                  w-2
                  rounded-full
                  bg-emerald-400
                "
              />

              <span>
                AI Online
              </span>
            </motion.div>

            {/* --------------------------------------------------
                SPARKLE ACCENT
            -------------------------------------------------- */}

            <motion.div
              aria-hidden="true"
              animate={{
                rotate: [0, 180, 360],
                scale: [1, 1.15, 1],
              }}
              transition={{
                duration: 5,
                repeat: Infinity,
                ease: "linear",
              }}
              className="
                pointer-events-none
                absolute
                -right-1
                -top-2
                text-[#F4B81A]
              "
            >
              <Sparkles
                size={18}
                fill="currentColor"
              />
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* ======================================================
          CHAT WINDOW
      ====================================================== */}

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{
              opacity: 0,
              y: 35,
              scale: 0.92,
            }}
            animate={{
              opacity: 1,
              y: 0,
              scale: 1,
            }}
            exit={{
              opacity: 0,
              y: 35,
              scale: 0.92,
            }}
            transition={{
              duration: 0.3,
              ease: "easeOut",
            }}
            className="
              fixed
              bottom-5
              right-5
              z-[999]
              flex
              h-[min(680px,calc(100vh-40px))]
              w-[min(430px,calc(100vw-24px))]
              flex-col
              overflow-hidden
              rounded-[28px]
              border
              border-white/20
              bg-white
              shadow-[0_25px_80px_rgba(7,19,48,0.35)]
              sm:bottom-6
              sm:right-6
            "
            role="dialog"
            aria-modal="false"
            aria-label="MukondoGTech AI Assistant"
          >
            {/* --------------------------------------------------
                HEADER
            -------------------------------------------------- */}

            <div
              className="
                relative
                flex
                min-h-[76px]
                shrink-0
                items-center
                justify-between
                overflow-hidden
                bg-gradient-to-r
                from-[#071330]
                via-[#0B1736]
                to-[#183B6B]
                px-5
                text-white
              "
            >
              {/* Header glow */}

              <motion.div
                aria-hidden="true"
                animate={{
                  x: ["-20%", "120%"],
                }}
                transition={{
                  duration: 5,
                  repeat: Infinity,
                  ease: "linear",
                }}
                className="
                  absolute
                  inset-y-0
                  w-32
                  bg-gradient-to-r
                  from-transparent
                  via-[#F4B81A]/10
                  to-transparent
                  blur-xl
                "
              />

              {/* Header content */}

              <div className="relative z-10 flex items-center gap-3">
                {/* Animated bot icon */}

                <motion.div
                  animate={{
                    y: [0, -2, 0],
                  }}
                  transition={{
                    duration: 2,
                    repeat: Infinity,
                    ease: "easeInOut",
                  }}
                  className="
                    relative
                    flex
                    h-11
                    w-11
                    shrink-0
                    items-center
                    justify-center
                    rounded-2xl
                    bg-gradient-to-br
                    from-[#F4B81A]
                    to-[#FFD96A]
                    text-[#071330]
                    shadow-lg
                  "
                >
                  <Bot
                    size={22}
                    strokeWidth={2.3}
                  />

                  {/* Live dot */}

                  <motion.span
                    animate={{
                      scale: [1, 1.3, 1],
                    }}
                    transition={{
                      duration: 1.5,
                      repeat: Infinity,
                    }}
                    className="
                      absolute
                      -right-1
                      -top-1
                      h-3
                      w-3
                      rounded-full
                      border-2
                      border-[#071330]
                      bg-emerald-400
                    "
                  />
                </motion.div>

                <div>
                  <div className="flex items-center gap-2">
                    <p className="font-bold">
                      MukondoGTech AI
                    </p>

                    <Sparkles
                      size={14}
                      className="text-[#F4B81A]"
                    />
                  </div>

                  <div className="mt-0.5 flex items-center gap-2">
                    <motion.span
                      animate={{
                        opacity: [1, 0.4, 1],
                      }}
                      transition={{
                        duration: 1.5,
                        repeat: Infinity,
                      }}
                      className="
                        h-2
                        w-2
                        rounded-full
                        bg-emerald-400
                      "
                    />

                    <p className="text-xs text-slate-300">
                      Immigration Assistant • Online
                    </p>
                  </div>
                </div>
              </div>

              {/* Close button */}

              <motion.button
                type="button"
                onClick={handleClose}
                whileHover={{
                  scale: 1.08,
                  rotate: 90,
                }}
                whileTap={{
                  scale: 0.92,
                }}
                aria-label="Close AI Assistant"
                title="Close AI Assistant"
                className="
                  relative
                  z-10
                  flex
                  h-10
                  w-10
                  items-center
                  justify-center
                  rounded-xl
                  text-slate-300
                  transition
                  hover:bg-white/10
                  hover:text-white
                  focus-visible:outline-none
                  focus-visible:ring-2
                  focus-visible:ring-[#F4B81A]
                "
              >
                <X size={21} />
              </motion.button>
            </div>

            {/* --------------------------------------------------
                CHAT CONTENT
            -------------------------------------------------- */}

            <div className="min-h-0 flex-1 bg-[#F8F6F1]">
              <AIChatWidgetContent />
            </div>

            {/* --------------------------------------------------
                BOTTOM ACCENT
            -------------------------------------------------- */}

            <motion.div
              aria-hidden="true"
              animate={{
                backgroundPosition: [
                  "0% 50%",
                  "100% 50%",
                  "0% 50%",
                ],
              }}
              transition={{
                duration: 5,
                repeat: Infinity,
                ease: "linear",
              }}
              className="
                h-1
                shrink-0
                bg-gradient-to-r
                from-[#F4B81A]
                via-[#2563EB]
                to-[#F4B81A]
                bg-[length:200%_100%]
              "
            />
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}


import { Rocket, CheckCircle } from "lucide-react";

export default function OnboardingPage() {
  const onboardingSteps = [
    "Complete your profile",
    "Upload your first document",
    "Explore AI analysis",
  ];

  return (
    <div
      className="
        min-h-screen
        bg-[#F8F6F1]
        flex
        items-center
        justify-center
        p-6
      "
    >
      <div
        className="
          max-w-xl
          w-full
          bg-white
          rounded-3xl
          border
          border-gray-200
          p-10
          text-center
          shadow-sm
        "
      >
        <Rocket
          size={60}
          className="
            mx-auto
            text-[#F4B81A]
          "
        />

        <h1
          className="
            text-4xl
            font-bold
            mt-6
          "
        >
          Welcome to MukondoGTech AI
        </h1>

        <p
          className="
            text-gray-600
            mt-4
          "
        >
          Let's setup your account and start analysing documents.
        </p>

        <div
          className="
            mt-8
            space-y-3
            text-left
          "
        >
          {onboardingSteps.map((item) => (
            <div
              key={item}
              className="
                flex
                items-center
                gap-3
              "
            >
              <CheckCircle className="text-green-600 flex-shrink-0" />

              <span>{item}</span>
            </div>
          ))}
        </div>

        <button
          type="button"
          className="
            mt-8
            bg-[#F4B81A]
            px-8
            py-3
            rounded-xl
            font-semibold
            transition-colors
            hover:opacity-90
          "
        >
          Get Started
        </button>
      </div>
    </div>
  );
}
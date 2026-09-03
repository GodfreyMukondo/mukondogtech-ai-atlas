import {
  BriefcaseBusiness,
  UserRound,
  Building2,
  Plane,
  ArrowRight,
} from "lucide-react";

const useCases = [
  {
    icon: UserRound,
    title: "Visa Applicants",
    description:
      "Understand your immigration documents, identify missing information, and prepare applications with greater confidence.",
  },
  {
    icon: BriefcaseBusiness,
    title: "Immigration Consultants",
    description:
      "Reduce manual document reviews and help clients process applications faster with AI-powered analysis.",
  },
  {
    icon: Building2,
    title: "Organizations",
    description:
      "Manage employee immigration documents securely with intelligent verification and reporting.",
  },
  {
    icon: Plane,
    title: "Travel & Relocation Services",
    description:
      "Improve document screening workflows and deliver faster customer support.",
  },
];


export default function UseCasesSection() {
  return (
    <section
      className="
        py-24
        bg-white
      "
    >

      <div
        className="
          max-w-7xl
          mx-auto
          px-6
        "
      >

        <div
          className="
            text-center
            max-w-3xl
            mx-auto
          "
        >

          <h2
            className="
              text-4xl
              md:text-5xl
              font-bold
              text-[#0B1736]
            "
          >
            Built for everyone involved in immigration
          </h2>


          <p
            className="
              mt-5
              text-lg
              text-[#7D8CA3]
            "
          >
            MukondoGTech AI helps individuals and
            organizations simplify document verification
            through intelligent automation.
          </p>

        </div>



        <div
          className="
            mt-14
            grid
            grid-cols-1
            md:grid-cols-2
            lg:grid-cols-4
            gap-6
          "
        >

          {useCases.map((item) => {

            const Icon = item.icon;

            return (
              <div
                key={item.title}
                className="
                  rounded-3xl
                  border
                  border-[#E5DED1]
                  bg-[#F8F6F1]
                  p-7
                  hover:shadow-lg
                  transition
                "
              >

                <div
                  className="
                    w-12
                    h-12
                    rounded-xl
                    bg-[#071330]
                    flex
                    items-center
                    justify-center
                  "
                >
                  <Icon
                    size={22}
                    className="text-[#F4B81A]"
                  />
                </div>


                <h3
                  className="
                    mt-6
                    text-xl
                    font-semibold
                    text-[#0B1736]
                  "
                >
                  {item.title}
                </h3>


                <p
                  className="
                    mt-3
                    text-[#7D8CA3]
                    leading-relaxed
                  "
                >
                  {item.description}
                </p>


                <ArrowRight
                  className="
                    mt-5
                    text-[#F4B81A]
                  "
                  size={20}
                />

              </div>
            );
          })}

        </div>

      </div>

    </section>
  );
}
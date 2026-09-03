import {
  useState,
} from "react";

import {
  Link,
} from "react-router-dom";

import {
  Check,
  ScanSearch,
  Sparkles,
  ShieldCheck,
  Zap,
} from "lucide-react";

import {
  motion,
} from "framer-motion";



type Plan = {
  name: string;
  description: string;
  monthly: number;
  yearly: number;
  popular?: boolean;
  features: string[];
};



const plans: Plan[] = [

  {
    name: "Starter",
    description:
      "Perfect for individuals analyzing occasional documents.",

    monthly: 9,
    yearly: 90,

    features: [
      "20 AI document analyses/month",
      "Passport & ID extraction",
      "Basic AI recommendations",
      "Document quality checks",
      "Email support",
    ],
  },


  {
    name: "Professional",

    description:
      "For professionals handling frequent immigration documents.",

    monthly: 29,
    yearly: 290,

    popular: true,

    features: [
      "200 AI document analyses/month",
      "Advanced AI verification",
      "Visa requirement analysis",
      "Document risk detection",
      "Priority support",
      "Export AI reports",
    ],
  },


  {
    name: "Enterprise",

    description:
      "For agencies managing large volumes of applications.",

    monthly: 99,
    yearly: 990,


    features: [
      "Unlimited team members",
      "Unlimited document analysis",
      "Advanced fraud detection",
      "Custom AI workflows",
      "API access",
      "Dedicated support",
    ],
  },

];



export default function PricingPage() {


  const [
    yearly,
    setYearly,
  ] = useState(false);



  return (

    <main
      className="
        min-h-screen
        bg-[#F8F6F1]
        text-[#0B1736]
      "
    >


      {/* Header */}

      <section
        className="
          pt-20
          pb-12
          px-6
          text-center
        "
      >


        <Link
          to="/"
          className="
            inline-flex
            items-center
            gap-3
            mb-10
          "
        >

          <div
            className="
              w-12
              h-12
              rounded-xl
              bg-white
              border
              border-[#E5DED1]
              flex
              items-center
              justify-center
            "
          >

            <ScanSearch
              className="
                text-[#F4B81A]
              "
            />

          </div>


          <span
            className="
              text-2xl
              font-bold
            "
          >

            MukondoGTech

            <span
              className="
                text-[#F4B81A]
              "
            >
              AI
            </span>

          </span>


        </Link>




        <motion.h1
          initial={{
            opacity:0,
            y:20,
          }}

          animate={{
            opacity:1,
            y:0,
          }}

          className="
            text-4xl
            md:text-6xl
            font-extrabold
          "
        >

          Simple pricing for
          <span
            className="
              text-[#F4B81A]
            "
          >
            {" "}AI-powered
          </span>

          <br />

          document analysis

        </motion.h1>



        <p
          className="
            mt-6
            max-w-2xl
            mx-auto
            text-[#7D8CA3]
            text-lg
          "
        >

          Analyze immigration documents,
          detect issues, and get AI-powered
          recommendations faster.

        </p>



        {/* Toggle */}

        <div
          className="
            mt-10
            inline-flex
            bg-white
            rounded-full
            p-1
            border
            border-[#E5DED1]
          "
        >

          <button
            onClick={() => setYearly(false)}
            className={`
              px-6
              py-2
              rounded-full
              font-semibold
              transition
              ${
                !yearly
                ?
                "bg-[#0B1736] text-white"
                :
                "text-[#7D8CA3]"
              }
            `}
          >
            Monthly
          </button>


          <button
            onClick={() => setYearly(true)}
            className={`
              px-6
              py-2
              rounded-full
              font-semibold
              transition
              ${
                yearly
                ?
                "bg-[#0B1736] text-white"
                :
                "text-[#7D8CA3]"
              }
            `}
          >

            Yearly
            <span
              className="
                ml-2
                text-[#F4B81A]
              "
            >
              Save 20%
            </span>

          </button>


        </div>


      </section>





      {/* Pricing Cards */}

      <section
        className="
          px-6
          pb-20
        "
      >

        <div
          className="
            max-w-7xl
            mx-auto
            grid
            md:grid-cols-3
            gap-8
          "
        >


          {plans.map(
            (plan) => (

            <motion.div

              key={plan.name}

              whileHover={{
                y:-8,
              }}

              className={`
                relative
                bg-white
                rounded-3xl
                p-8
                border
                ${
                  plan.popular
                  ?
                  "border-[#F4B81A] shadow-xl"
                  :
                  "border-[#E5DED1]"
                }
              `}
            >


              {plan.popular && (

                <div
                  className="
                    absolute
                    -top-4
                    left-1/2
                    -translate-x-1/2
                    bg-[#F4B81A]
                    text-[#0B1736]
                    px-5
                    py-1
                    rounded-full
                    text-sm
                    font-bold
                  "
                >

                  Most Popular

                </div>

              )}





              <h2
                className="
                  text-2xl
                  font-bold
                "
              >

                {plan.name}

              </h2>



              <p
                className="
                  mt-3
                  text-[#7D8CA3]
                  min-h-14
                "
              >

                {plan.description}

              </p>




              <div
                className="
                  mt-6
                "
              >

                <span
                  className="
                    text-5xl
                    font-extrabold
                  "
                >

                  ${yearly ? plan.yearly : plan.monthly}

                </span>


                <span
                  className="
                    text-[#7D8CA3]
                  "
                >

                  /{yearly ? "year" : "month"}

                </span>


              </div>




              <Link
                to="/register"
                className="
                  mt-8
                  block
                  text-center
                  bg-[#0B1736]
                  text-white
                  py-3
                  rounded-xl
                  font-semibold
                  hover:bg-[#152650]
                  transition
                "
              >

                Start Free Trial

              </Link>





              <ul
                className="
                  mt-8
                  space-y-4
                "
              >

                {plan.features.map(
                  feature => (

                  <li
                    key={feature}
                    className="
                      flex
                      gap-3
                      text-[#44546A]
                    "
                  >

                    <Check
                      size={20}
                      className="
                        text-green-600
                        shrink-0
                      "
                    />

                    {feature}

                  </li>

                ))}

              </ul>


            </motion.div>

          ))}


        </div>


      </section>






      {/* Trust Section */}

      <section
        className="
          bg-white
          py-16
          px-6
        "
      >

        <div
          className="
            max-w-4xl
            mx-auto
            grid
            md:grid-cols-3
            gap-8
            text-center
          "
        >

          <div>
            <ShieldCheck
              className="
                mx-auto
                text-[#F4B81A]
              "
            />

            <h3
              className="
                mt-3
                font-bold
              "
            >
              Secure Documents
            </h3>

            <p
              className="
                text-sm
                text-[#7D8CA3]
              "
            >
              Enterprise-grade document protection.
            </p>

          </div>



          <div>

            <Sparkles
              className="
                mx-auto
                text-[#F4B81A]
              "
            />

            <h3
              className="
                mt-3
                font-bold
              "
            >
              AI Powered
            </h3>

            <p
              className="
                text-sm
                text-[#7D8CA3]
              "
            >
              Advanced AI document intelligence.
            </p>

          </div>



          <div>

            <Zap
              className="
                mx-auto
                text-[#F4B81A]
              "
            />

            <h3
              className="
                mt-3
                font-bold
              "
            >
              Fast Results
            </h3>

            <p
              className="
                text-sm
                text-[#7D8CA3]
              "
            >
              Receive analysis in seconds.
            </p>

          </div>


        </div>


      </section>



    </main>

  );
}
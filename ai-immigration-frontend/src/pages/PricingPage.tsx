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
        text-slate-100
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
              bg-white/5
              backdrop-blur-sm
              border
              border-white/10
              flex
              items-center
              justify-center
            "
          >

            <ScanSearch
              className="
                text-[#C6A15B]
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
                text-[#C6A15B]
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
              text-[#C6A15B]
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
            text-slate-300
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
            bg-white/5
            backdrop-blur-sm
            rounded-full
            p-1
            border
            border-white/10
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
                "bg-gradient-to-r from-[#C6A15B] to-[#A8894D] text-[#071426]"
                :
                "text-slate-400"
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
                "bg-gradient-to-r from-[#C6A15B] to-[#A8894D] text-[#071426]"
                :
                "text-slate-400"
              }
            `}
          >

            Yearly
            <span
              className="
                ml-2
                text-[#C6A15B]
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
                bg-white/5
                backdrop-blur-xl
                rounded-3xl
                p-8
                border
                ${
                  plan.popular
                  ?
                  "border-[#C6A15B] shadow-xl"
                  :
                  "border-white/10"
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
                    bg-[#C6A15B]
                    text-[#0B1F3A]
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
                  text-white
                "
              >

                {plan.name}

              </h2>



              <p
                className="
                  mt-3
                  text-slate-300
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
                    text-white
                  "
                >

                  ${yearly ? plan.yearly : plan.monthly}

                </span>


                <span
                  className="
                    text-slate-400
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
                  bg-gradient-to-r
                  from-[#C6A15B]
                  to-[#A8894D]
                  text-[#071426]
                  py-3
                  rounded-xl
                  font-semibold
                  hover:opacity-90
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
                      text-slate-300
                    "
                  >

                    <Check
                      size={20}
                      className="
                        text-emerald-400
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
          bg-white/5
          backdrop-blur-xl
          border-y
          border-white/10
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
                text-[#C6A15B]
              "
            />

            <h3
              className="
                mt-3
                font-bold
                text-white
              "
            >
              Secure Documents
            </h3>

            <p
              className="
                text-sm
                text-slate-400
              "
            >
              Enterprise-grade document protection.
            </p>

          </div>



          <div>

            <Sparkles
              className="
                mx-auto
                text-[#C6A15B]
              "
            />

            <h3
              className="
                mt-3
                font-bold
                text-white
              "
            >
              AI Powered
            </h3>

            <p
              className="
                text-sm
                text-slate-400
              "
            >
              Advanced AI document intelligence.
            </p>

          </div>



          <div>

            <Zap
              className="
                mx-auto
                text-[#C6A15B]
              "
            />

            <h3
              className="
                mt-3
                font-bold
                text-white
              "
            >
              Fast Results
            </h3>

            <p
              className="
                text-sm
                text-slate-400
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
import { Check, ArrowRight } from "lucide-react";
import { Link } from "react-router-dom";
import { useAuth } from "../../features/auth/hooks/useAuth";


const features = [
  "AI document analysis",
  "Secure file processing",
  "Verification reports",
  "AI assistance",
];


export default function PricingPreview() {

  const { user } = useAuth();


  const destination = user
    ? "/dashboard/upload"
    : "/register";


  return (

    <section
      id="pricing"
      className="
        py-24
      "
    >

      <div
        className="
          max-w-5xl
          mx-auto
          px-6
          text-center
        "
      >


        <h2
          className="
            text-4xl
            md:text-5xl
            font-bold
            text-white
          "
        >
          Simple pricing for smarter verification
        </h2>



        <p
          className="
            mt-5
            max-w-2xl
            mx-auto
            text-lg
            text-slate-300
          "
        >
          Start with powerful AI immigration document
          intelligence and scale as your verification
          needs grow.
        </p>





        <div
          className="
            mt-12
            max-w-md
            mx-auto
            rounded-3xl
            border
            border-white/10
            bg-white/5
            backdrop-blur-xl
            p-8
            shadow-2xl
            shadow-black/40
            transition-all
            duration-300
            hover:-translate-y-2
          "
        >


          <div
            className="
              inline-flex
              rounded-full
              bg-[#C6A15B]/15
              px-4
              py-2
              text-sm
              font-semibold
              text-[#C6A15B]
            "
          >
            Start Free
          </div>



          <h3
            className="
              mt-6
              text-3xl
              font-bold
              text-white
            "
          >
            Free Plan
          </h3>



          <p
            className="
              mt-3
              leading-relaxed
              text-slate-300
            "
          >
            Explore MukondoGTech AI document
            intelligence before upgrading.
          </p>





          <div
            className="
              mt-8
              space-y-4
              text-left
            "
          >

            {features.map((feature)=>(

              <div
                key={feature}
                className="
                  flex
                  items-center
                  gap-3
                  text-white
                "
              >

                <div
                  className="
                    flex
                    h-6
                    w-6
                    items-center
                    justify-center
                    rounded-full
                    bg-emerald-500/15
                  "
                >

                  <Check
                    size={15}
                    className="text-emerald-400"
                  />

                </div>


                <span
                  className="
                    text-sm
                    font-medium
                  "
                >
                  {feature}
                </span>


              </div>

            ))}

          </div>





          <Link
            to={destination}
            className="
              mt-10
              flex
              w-full
              items-center
              justify-center
              gap-2
              rounded-xl
              bg-gradient-to-r
              from-[#C6A15B]
              to-[#D4B984]
              py-4
              font-bold
              text-black
              shadow-lg
              transition-all
              duration-300
              hover:-translate-y-1
              hover:shadow-xl
            "
          >

            Get Started

            <ArrowRight size={18}/>

          </Link>


        </div>


      </div>


    </section>

  );
}
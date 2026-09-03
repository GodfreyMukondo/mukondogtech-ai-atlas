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
        bg-[#F8F6F1]
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
            text-[#0B1736]
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
            text-[#7D8CA3]
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
            border-[#E5DED1]
            bg-white
            p-8
            shadow-xl
            transition-all
            duration-300
            hover:-translate-y-2
          "
        >


          <div
            className="
              inline-flex
              rounded-full
              bg-[#FFF4D1]
              px-4
              py-2
              text-sm
              font-semibold
              text-[#B58100]
            "
          >
            Start Free
          </div>



          <h3
            className="
              mt-6
              text-3xl
              font-bold
              text-[#0B1736]
            "
          >
            Free Plan
          </h3>



          <p
            className="
              mt-3
              leading-relaxed
              text-[#7D8CA3]
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
                  text-[#0B1736]
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
                    bg-green-100
                  "
                >

                  <Check
                    size={15}
                    className="text-green-600"
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
              from-[#F4B81A]
              to-[#FFD96A]
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
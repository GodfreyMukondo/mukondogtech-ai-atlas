import {
  ButtonHTMLAttributes,
  ReactNode,
} from "react";


interface ButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement> {

  children: ReactNode;

  variant?:
     "primary"
     "secondary"
     "danger"
     "outline"
     "ghost";

  size?:
     "sm"
     "md"
     "lg";

  loading?: boolean;
}


export default function Button({
  children,
  variant = "primary",
  size = "md",
  loading = false,
  disabled,
  className = "",
  ...props
}: ButtonProps) {


  const variants = {

    primary:
      "bg-[#F4B81A] text-black hover:bg-[#E5AA12]",

    secondary:
      "bg-[#071330] text-white hover:bg-[#0B1736]",

    danger:
      "bg-red-500 text-white hover:bg-red-600",

    outline:
      "border border-[#D9DDE5] text-[#0B1736] hover:bg-gray-50",

    ghost:
      "text-[#0B1736] hover:bg-gray-100",

  };


  const sizes = {

    sm:
      "px-3 py-2 text-sm",

    md:
      "px-5 py-3",

    lg:
      "px-8 py-4 text-lg",

  };


  return (

    <button

      disabled={
        disabled || loading
      }

      className={`
        rounded-xl
        font-semibold
        transition
        inline-flex
        items-center
        justify-center
        gap-2
        disabled:opacity-60
        disabled:cursor-not-allowed

        ${variants[variant]}

        ${sizes[size]}

        ${className}
      `}

      {...props}

    >

      {loading
        ? "Loading..."
        : children
      }

    </button>

  );
}
import type {
  ButtonHTMLAttributes,
  ReactNode,
} from "react";


interface ButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement> {

  children: ReactNode;

  variant?:
     | "primary"
     | "secondary"
     | "danger"
     | "outline"
     | "ghost";

  size?:
     | "sm"
     | "md"
     | "lg";

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
      "bg-[#C6A15B] text-black hover:bg-[#A8894D]",

    secondary:
      "border border-white/15 bg-white/10 text-white hover:bg-white/20",

    danger:
      "bg-red-500 text-white hover:bg-red-600",

    outline:
      "border border-white/20 text-white hover:bg-white/10",

    ghost:
      "text-white hover:bg-white/10",

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
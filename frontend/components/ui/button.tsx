import * as React from "react";
import { cn } from "@/lib/cn";

type ButtonVariant = "default" | "secondary" | "outline" | "destructive";

type ButtonProps = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
};

const variantClasses: Record<ButtonVariant, string> = {
  default:
    "bg-slate-900 text-white hover:bg-slate-700 disabled:bg-slate-400 disabled:text-slate-100",
  secondary:
    "bg-cyan-600 text-white hover:bg-cyan-500 disabled:bg-cyan-300",
  outline:
    "border border-slate-300 bg-white text-slate-800 hover:bg-slate-50 disabled:text-slate-400",
  destructive:
    "bg-rose-600 text-white hover:bg-rose-500 disabled:bg-rose-300",
};

export function Button({ className, variant = "default", ...props }: ButtonProps) {
  return (
    <button
      className={cn(
        "inline-flex h-10 items-center justify-center rounded-xl px-4 text-sm font-medium transition disabled:cursor-not-allowed",
        variantClasses[variant],
        className,
      )}
      {...props}
    />
  );
}

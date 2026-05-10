import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "DIST-MAIL Dashboard",
  description: "Realtime control dashboard for distributed email dispatch simulation",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="h-full antialiased">
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}

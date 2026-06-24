import React from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import "@/App.css";
import Shell from "@/components/swurlz/Shell";
import Home from "@/pages/Home";
import Cockpit from "@/pages/Cockpit";
import Skills from "@/pages/Skills";
import Lab from "@/pages/Lab";

export default function App() {
  return (
    <BrowserRouter>
      <Shell>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/cockpit" element={<Cockpit />} />
          <Route path="/skills" element={<Skills />} />
          <Route path="/lab" element={<Lab />} />
        </Routes>
      </Shell>
    </BrowserRouter>
  );
}

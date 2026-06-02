"use client";

import { Float, MeshDistortMaterial, OrbitControls, Stars } from "@react-three/drei";
import { Canvas } from "@react-three/fiber";
import { useReducedMotion } from "framer-motion";
import { Suspense } from "react";

function CoreObject({ animate }: { animate: boolean }) {
  return (
    <Float speed={animate ? 1.25 : 0} rotationIntensity={animate ? 0.34 : 0} floatIntensity={animate ? 0.65 : 0}>
      <mesh rotation={[0.35, 0.72, 0.1]}>
        <icosahedronGeometry args={[1.9, 4]} />
        <MeshDistortMaterial color="#2563eb" distort={0.24} speed={animate ? 1 : 0} roughness={0.22} metalness={0.82} />
      </mesh>
    </Float>
  );
}

function Rings() {
  return (
    <group>
      {[2.75, 3.8, 4.9].map((radius, index) => (
        <mesh key={radius} rotation={[Math.PI / 2.35, index * 0.29, index * 0.4]}>
          <torusGeometry args={[radius, index === 1 ? 0.009 : 0.006, 16, 200]} />
          <meshBasicMaterial color={index === 1 ? "#ffffff" : "#4b7df1"} transparent opacity={index === 1 ? 0.34 : 0.3} />
        </mesh>
      ))}
    </group>
  );
}

export function HeroScene() {
  const reduceMotion = useReducedMotion();
  const animate = !reduceMotion;

  return (
    <div className="hero-canvas">
      <Canvas camera={{ position: [1.5, 0, 8.9], fov: 43 }} dpr={[1, 1.6]}>
        <Suspense fallback={null}>
          <ambientLight intensity={0.45} />
          <pointLight position={[5, 3, 5]} intensity={4} color="#dbe7ff" />
          <pointLight position={[-3, -2, 3]} intensity={2.5} color="#2563eb" />
          <Stars radius={72} depth={45} count={760} factor={3.1} saturation={0} fade speed={animate ? 0.26 : 0} />
          <CoreObject animate={animate} />
          <Rings />
          <OrbitControls enableZoom={false} enablePan={false} autoRotate={animate} autoRotateSpeed={0.38} />
        </Suspense>
      </Canvas>
    </div>
  );
}

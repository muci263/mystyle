"use client";

import { useEffect, useRef } from "react";

const trailCount = 7;

export function PointerTrail() {
  const rootRef = useRef<HTMLDivElement>(null);
  const headRef = useRef<HTMLSpanElement>(null);
  const segmentRefs = useRef<Array<HTMLSpanElement | null>>([]);

  useEffect(() => {
    const root = rootRef.current;
    const head = headRef.current;
    const segments = segmentRefs.current.filter((segment): segment is HTMLSpanElement => segment !== null);
    const supportsPointerTrail = window.matchMedia("(pointer: fine)").matches;
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

    if (!root || !head || segments.length !== trailCount || !supportsPointerTrail || reduceMotion) return;

    const target = { x: -100, y: -100 };
    const headPoint = { x: -100, y: -100 };
    const points = Array.from({ length: trailCount }, () => ({ x: -100, y: -100 }));
    let initialized = false;
    let frameId = 0;

    const paint = () => {
      headPoint.x += (target.x - headPoint.x) * 0.56;
      headPoint.y += (target.y - headPoint.y) * 0.56;
      head.style.transform = `translate3d(${headPoint.x - 4}px, ${headPoint.y - 4}px, 0)`;

      let anchor = headPoint;
      segments.forEach((segment, index) => {
        const point = points[index];
        const pull = 0.34 - index * 0.023;
        point.x += (anchor.x - point.x) * pull;
        point.y += (anchor.y - point.y) * pull;
        const angle = Math.atan2(anchor.y - point.y, anchor.x - point.x);
        segment.style.transform = `translate3d(${point.x}px, ${point.y}px, 0) rotate(${angle}rad)`;
        anchor = point;
      });

      frameId = window.requestAnimationFrame(paint);
    };

    const showTrail = (event: PointerEvent) => {
      if (event.pointerType === "touch") return;
      target.x = event.clientX;
      target.y = event.clientY;

      if (!initialized) {
        initialized = true;
        headPoint.x = target.x;
        headPoint.y = target.y;
        points.forEach((point) => {
          point.x = target.x;
          point.y = target.y;
        });
        frameId = window.requestAnimationFrame(paint);
      }

      root.dataset.visible = "true";
    };

    const hideTrail = () => {
      root.dataset.visible = "false";
    };

    window.addEventListener("pointermove", showTrail, { passive: true });
    document.documentElement.addEventListener("pointerleave", hideTrail);
    window.addEventListener("blur", hideTrail);

    return () => {
      window.removeEventListener("pointermove", showTrail);
      document.documentElement.removeEventListener("pointerleave", hideTrail);
      window.removeEventListener("blur", hideTrail);
      window.cancelAnimationFrame(frameId);
    };
  }, []);

  return (
    <div ref={rootRef} className="pointer-trail" data-visible="false" aria-hidden="true">
      <span ref={headRef} className="pointer-trail-head" />
      {Array.from({ length: trailCount }, (_, index) => (
        <span
          key={index}
          ref={(node) => {
            segmentRefs.current[index] = node;
          }}
          className={`pointer-trail-streak pointer-trail-streak-${index + 1}`}
        />
      ))}
    </div>
  );
}

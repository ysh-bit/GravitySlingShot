# Gravity Slingshot — Orbital Physics Engine

A pure Java Swing 2D orbital mechanics puzzle game built using Object-Oriented Programming principles, custom 2D graphics, multithreading, and file I/O persistence.

## Syllabus Architecture Mapping
* **OOP & Encapsulation:** `Vector2D` physics math, constructor overloading, `final` constants, member inner class (`Planet.GravityWell`), and static nested class (`TelemetryManager.MissionRecord`).
* **Inheritance & Interfaces:** Abstract `SpaceEntity` extended by `Planet`, `Probe`, and `Wormhole`, implementing `Renderable` and `Gravitational`.
* **Multithreading & Exceptions:** Dedicated 60 FPS physics thread (`Runnable`), background **Daemon Thread** for live trajectory prediction, `synchronized` state locks, and custom `FuelDepletedException` / `BlackHoleCollisionException`.
* **I/O, Events & Swing MVC:** `BufferedWriter`/`BufferedReader` telemetry logging, `MouseAdapter`/`MouseMotionAdapter` drag-and-aim controls, `JMenuBar`, `JComboBox`, `JProgressBar`, and custom `Graphics2D` rendering.

## Quick Start (Single Command)
Ensure JDK 17+ is installed, then run:
```bash
javac GravitySlingshot.java && java GravitySlingshot
```

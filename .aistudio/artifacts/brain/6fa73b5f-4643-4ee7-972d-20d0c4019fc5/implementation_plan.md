# Neon CyberPuck: 3D Air Hockey & Pinball Arena

An arcade-action hybrid combining the intense, direct striker control of air hockey with the kinetic obstacles, score multipliers, and energetic rebound bumpers of classic 3D pinball, built for local head-to-head multiplayer on Android.

### User Review & Critical Decisions

> [!IMPORTANT]
> Based on your selections, the game will feature a **tilted 3D neon arcade perspective**, **head-to-head split controls** on opposite ends of the screen, and active **pinball bumpers, angled slingshots, and score targets**. Review the confirmed architecture and decisions below before we begin implementation.

- **Confirmed Perspective**: Tilted 3D perspective with depth foreshortening, neon grid lines, rim glows, and dynamic motion blur.
- **Confirmed Player Setup**: Head-to-head local multiplayer (Player 1 at the bottom, Player 2 at the top, inverted for face-to-face play) plus an optional Solo Practice vs AI toggle.
- **Confirmed Pinball Elements**: Active kinetic mushroom bumpers with impulse pushback, angled slingshots on side walls, and light-up score targets.

---

### 1. Overview & Core Concept

- **What It Does**: Two players control cylindrical strikers on opposite ends of a tilted 3D neon air hockey table. Between the goals sits an obstacle course of pinball hazards: energetic bumpers that blast the puck back with bonus speed, reactive slingshots that deflect shots at sharp angles, and score targets that accumulate pinball points alongside traditional air hockey goal scoring.
- **Target Audience / Persona**: Casual gamers, friends playing together on a single phone or tablet, and arcade enthusiasts who love fast-paced physical reflexes and pinball trick shots.
- **Key Value**: Merges the strategic puck angling of air hockey with the chaotic, high-energy unpredictability of pinball physics in a zero-latency, tactile mobile experience.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Title & Match Setup**:
   - High-energy neon start screen with mode selection: **2-Player Head-to-Head** (opposite ends) or **Solo vs Cyber AI** (Easy, Medium, Hard).
   - Match configuration: Goal limit (First to 5, 7, or 10), obstacle layout selection (Standard Arena, Pinball Chaos, or Classic Hockey).
2. **The Arena & Head-to-Head Gameplay**:
   - The phone sits between two players on a table or in their hands.
   - Player 1 controls the cyan striker in the lower half; Player 2 controls the magenta/amber striker in the upper half (with inverted UI for upright viewing from their side).
   - Multi-touch system tracks both players simultaneously with zero input contention or pointer dropping.
   - Hitting pinball bumpers triggers screen shakes, particle bursts, and haptic pulses.
3. **Goal & Score Sequence**:
   - Puck crossing the goal line triggers neon strobe lighting, slow-motion impact freeze, particle fireworks, and goal siren haptics.
   - Round reset with a countdown and puck spawn at center table.
4. **Victory & Match Statistics**:
   - Win screen highlighting winner, total pinball bumper hits, max puck velocity recorded, and highest combo.

#### Visual Identity & Theme
- **Aesthetic Direction**: Cyberpunk Neon Synthwave / Retro-Futuristic Tron Arcade.
- **Color Palette**:
  - Table Background: Deep obsidian/void `#0B0C15` with subtle animated neon gridlines `#1A1D36`.
  - Player 1 (Cyan/Electric Blue): `#00F0FF` with neon trail and glow `#00A3FF`.
  - Player 2 (Neon Fuchsia/Magenta): `#FF007F` with neon glow `#FF3399`.
  - The Puck (Plasma Gold / White Core): `#FFFFFF` core with intense `#FFD700` / `#FF9100` halo.
  - Pinball Bumpers (Toxic Green & Hyper Violet): High-contrast pulsing rings that illuminate the table surface when struck.
- **Tilted 3D Projection & Camera**:
  - Custom Canvas mathematical projection transforming 2D table coordinates $(x, y)$ into a 2.5D tilted perspective $(x', y')$ with vanishing point scaling, puck shadow offset, and 3D beveled cylinders for strikers and bumpers.
- **Interactive Feedback**:
  - Real-time synthesised sound effects via Android `AudioTrack` (retro frequency sweeps, metallic clinks, bumper pop tones, and goal horns).
  - Native haptic feedback via `Vibrator` on bumper impacts, wall kicks, and goal events.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Custom Vector Physics Engine vs External Physics Libraries**
  - *Chosen Approach*: Lightweight, deterministic, allocation-free 2D impulse physics engine written specifically for air hockey and pinball geometry (circle-circle, circle-line segment, circle-capsule, and moving circle-circle with striker momentum transfer).
  - *Why*: Eliminates heavy native dependencies or JNI bindings (like Box2D / JBox2D) that complicate builds, ensures guaranteed 60-120fps on all Android devices, and allows seamless integration with touch dragging.
  - *Alternatives Considered*: Box2D or Chipmunk (excessive complexity and awkward touch-kinematic coupling for air hockey).

- **Decision 2: 3D Projection Rendering via Jetpack Compose Canvas**
  - *Chosen Approach*: Direct trigonometric perspective matrix projection in Compose `Canvas` with hardware-accelerated drawing primitives (`drawCircle`, `drawPath`, `drawPoints`, gradients, and blend modes).
  - *Why*: Zero OpenGL / Vulkan boilerplate; full lifecycle alignment with Compose; instant hot-reload and crisp vector rendering across all screen densities.
  - *Alternatives Considered*: SceneView / Filament (huge APK overhead, external asset pipelines, and slower load times).

- **Decision 3: Multi-Touch & Finger Tracking Strategy**
  - *Chosen Approach*: Multi-pointer tracking partitioned across mid-table boundaries. Player 1 touch events are clamped to $Y \in [0.5, 1.0]$; Player 2 touch events are clamped to $Y \in [0.0, 0.5]$.
  - *Why*: Prevents players from illegally invading the opponent's zone, maintains strict air hockey rules, and prevents pointer ID theft when both players tap at high frequencies.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                     MainActivity                       │
│             (Edge-to-Edge, Theme, Audio, Haptics)     │
└──────────────────────────┬─────────────────────────────┘
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
┌───────────────────────────┐ ┌──────────────────────────┐
│        GameScreen         │ │    Title & Pause UI      │
│  (Arena Canvas & UI HUD)  │ │ (Mode, Goal Limit, AI)   │
└────────────┬──────────────┘ └──────────────────────────┘
             │
             ▼
┌────────────────────────────────────────────────────────┐
│                     GameViewModel                      │
│ - GameState (Playing, GoalScored, Paused, GameOver)    │
│ - Player Scores, Goal Counters, Combos, Timers         │
│ - Solo AI Controller (Reactive positioning & striking) │
└──────────────────────────┬─────────────────────────────┘
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
┌───────────────────────────┐ ┌──────────────────────────┐
│       PhysicsEngine       │ │      ArcadeAudio         │
│ - Continuous Puck Motion  │ │ - Tone Synthesis Engine  │
│ - Striker Touch Velocity  │ │ - Haptic Vibration FX    │
│ - Bumper Impulse Push     │ └──────────────────────────┘
│ - Slingshot Deflections   │
│ - Goal Boundary Detection │
└───────────────────────────┘
```

#### Core Components & Modules
1. **Physics & Math Layer (`PhysicsEngine.kt`, `Vector2D.kt`, `Obstacles.kt`)**:
   - `Vector2D`: Lightweight 2D vector primitives for dot products, reflection, length, normalisation.
   - `Puck`: Position, velocity, radius, mass, friction damping, speed cap, and trail history.
   - `Striker`: Target position, velocity vector derived from finger velocity, radius, mass, and boundary containment.
   - `PinballBumper`: Circular mushroom bumper with reactive trigger state, light timer, pushback force, and point value.
   - `Slingshot`: Line/capsule angled barriers that violently propel pucks perpendicular to their surface.
   - `ScoreTarget`: Toggleable target targets that activate on puck impact.
2. **Visual & 3D Projection Layer (`ArenaRenderer.kt`, `PerspectiveProjection.kt`)**:
   - Translates top-down physics coordinates into tilted 3D space with trapezoidal perspective, dynamic shadows, glowing puck wake, and cylinder heights.
3. **Sound & Haptics (`ArcadeAudioEngine.kt`)**:
   - Low-latency synthesized sound generation via `AudioTrack` producing classic arcade square/sine chiptune effects without requiring bulky external media assets.
4. **State & UI Layer (`GameViewModel.kt`, `GameScreen.kt`)**:
   - Clean M3 styling, pause overlay, winner victory screen, sound/vibration toggles, and instant rematch controls.

# LiveKit Voice Agent Companion (Wave UI)

A native Android application providing a real-time, low-latency voice interaction experience with your PC/Cloud LiveKit agent, faithfully implementing the LiveKit Agents UI "Wave" visualizer design with dark luxury aesthetics and Phosphor-styled iconography.

## User Review & Critical Decisions

> [!IMPORTANT]
> The app is built specifically for native Android (APK) using Jetpack Compose, targeting real-time voice streaming with your LiveKit AI agent.

- **UI Style & Template**: LiveKit Agents UI Wave template with multi-harmonic oscillating sine waves, dynamic glow, color shifts, and smooth amplitude reactivity.
- **Iconography**: Phosphor Icons geometry (clean, precise line strokes with rounded joints; zero emojis).
- **Connection Pipeline**: Flexible connection manager supporting LiveKit Cloud WebSocket (`wss://...`) and local PC LAN endpoints (`ws://192.168.x.x:7880`), direct room token entry, and a built-in token server fetcher.
- **Audio Architecture**: Full-duplex audio stream handling with real-time mic amplitude analysis (`AudioRecord` / LiveKit audio track) and simulated fallback for offline testing.

---

## 1. Overview & Core Concept

- **What It Does**: Connects your Android phone to your LiveKit agent room. Allows you to speak naturally on the go while observing real-time voice activity through an oscilloscope-style wave visualizer, conversation status indicators (connecting, listening, thinking, speaking), live transcripts, and agent controls.
- **Target Audience**: Developers and users running local or cloud-hosted AI agents who want a dedicated Gemini Live style mobile companion.
- **Key Value**: Low-latency, hands-free voice conversation with visual feedback, intuitive controls, and seamless LAN or remote LiveKit connectivity.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Launch & Standby**:
   - The app opens into a sleek dark interface featuring the LiveKit Wave canvas in an idle floating wave rhythm.
   - Status badge displays current agent connection state with a soft glowing indicator.
2. **Connect & Session Start**:
   - Quick Connect sheet or Settings modal allows entering or switching between LiveKit server URL, Room name, and Token (with saved profiles for home PC and cloud).
   - Once connected, the room handshake transitions the state to `listening`.
3. **Voice Interaction (Gemini Live Style)**:
   - When the user speaks, local mic audio amplifies the wave in real-time with responsive crests.
   - When the agent responds, the wave shifts to vibrant cyan/electric blue (`#1FD5F9`) with multi-phase harmonic ripples matching the agent's voice.
   - An expandable bottom drawer provides real-time transcripts and agent tool action logs.
4. **Control Bar**:
   - Persistent bottom controls: Mic Mute/Unmute, End Call / Reconnect, Transcript Drawer toggle, Audio Output toggle (Speaker / Earpiece), and Settings.

### Visual Identity & Theme
- **Aesthetic**: Dark luxury, high-tech minimalism inspired by LiveKit Agents UI and Gemini Live.
- **Color Palette**:
  - Background: Deep slate/black `#0B0F17` to `#111827`
  - Wave Primary: Electric Cyan `#1FD5F9`
  - Wave Secondary / Shift: Vivid Purple/Indigo `#818CF8` & Teal `#2DD4BF`
  - Neutral Surface: Charcoal `#1E293B` and Slate `#334155`
  - Text: High-contrast white `#F8FAFC` and muted `#94A3B8`
- **Typography**: Clean modern sans-serif with clear optical weights and tabular figures for timer/latency metrics.
- **Icons**: Authentic Phosphor design language (24dp stroke icons: microphone, microphone-slash, waves, chat-teardrop-text, sliders-horizontal, gear, speaker-high, phone-x). Absolutely zero emojis.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Native Canvas Wave Visualizer vs Web View**
  - *Chosen Approach*: Pure native Jetpack Compose `Canvas` with multi-harmonic sine waves, phase interpolation, and blur/glow shader layer.
  - *Why*: Eliminates webview overhead, guarantees 60/120 FPS buttery-smooth rendering, and avoids heavy runtime dependencies.
  - *Alternatives Considered*: Web-based iframe with LiveKit React component (discarded because user explicitly requested APK native build).

- **Decision 2: Audio Level Processing & Visualizer Binding**
  - *Chosen Approach*: Combined real-time microphone amplitude detector (`AudioRecord` analyzer) + LiveKit agent audio subscriber callback.
  - *Why*: Provides immediate visual response with zero perceived lag when speaking, even before server round-trips.
  - *Alternatives Considered*: Server-only metrics (laggy visual feedback).

- **Decision 3: Phosphor Icons Rendering**
  - *Chosen Approach*: Clean Compose Vector paths and drawables faithfully matching Phosphor Icons official geometry.
  - *Why*: Keeps APK lightweight, crisp on all screen densities, and strictly avoids emojis.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌─────────────────────────────────────────────────────────┐
│                      MainActivity                       │
│                           │                             │
│                  LiveKitAgentTheme                      │
│                           │                             │
│               AgentVoiceScreen (Scaffold)               │
│  ┌───────────────────────────────────────────────────┐  │
│  │ TopBar: Connection Status Badge & Latency Ping     │  │
│  ├───────────────────────────────────────────────────┤  │
│  │ Center Stage:                                     │  │
│  │   AgentAudioVisualizerWave (Custom Canvas)        │  │
│  │   - Multi-harmonic animated waveforms             │  │
│  │   - Dynamic color shifting (#1FD5F9 / #818CF8)    │  │
│  │   - State animations (idle, thinking, speaking)   │  │
│  │   AgentStateLabel (listening, speaking, etc.)     │  │
│  ├───────────────────────────────────────────────────┤  │
│  │ Live Transcript Drawer (Collapsible)              │  │
│  │   - User & Agent dialog bubbles                   │  │
│  │   - Tool execution status tags                    │  │
│  ├───────────────────────────────────────────────────┤  │
│  │ AgentControlBar (Bottom Navigation Inset Safe)     │  │
│  │   - Mic Toggle (Phosphor Mic / MicSlash)          │  │
│  │   - End / Reconnect Session                       │  │
│  │   - Transcript View Toggle                        │  │
│  │   - Audio Route Switcher                          │  │
│  │   - Connection Settings Modal                     │  │
│  └───────────────────────────────────────────────────┘  │
│                           │                             │
│                 AgentViewModel (MVVM)                   │
│         ┌─────────────────┴─────────────────┐           │
│         ▼                                   ▼           │
│  LiveKitSessionManager             AudioLevelAnalyzer   │
│  - WebSocket / WebRTC Client       - Mic amplitude feed │
│  - Token Storage (DataStore/Pref)  - Wave FFT smoothing │
│  - Message / Transcript Flow                            │
└─────────────────────────────────────────────────────────┘
```

### State Models
- **AgentState**: `Disconnected`, `Connecting`, `Connected`, `Listening`, `Thinking`, `Speaking`
- **TranscriptEntry**: `id`, `speaker` (`User` | `Agent`), `text`, `timestamp`, `isFinal`
- **LiveKitConfig**: `serverUrl`, `token`, `roomName`, `tokenEndpoint`

---

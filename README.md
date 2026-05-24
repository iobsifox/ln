# Last Night

**Last Night** is a premium, high-performance, dark-neon-styled Android network and DNS-over-HTTPS container application. It provides client-side proxy servers and dynamic VPN routing capabilities natively for Android devices.

## 🌙 Design Theme & Atmosphere
The application features a modern, immersive, night-time cyber-neon aesthetic leveraging key primary color maps:
*   **Background:** `#070A12` (Cosmic Dark Slate)
*   **Surface:** `#101827`
*   **Card:** `#151F32`
*   **Neon Accents:** Cyan (`#22D3EE`), Light Blue (`#60A5FA`), Purple (`#A78BFA`)
*   **Status Codes:** Success (`#34D399`), Danger (`#FB7185`)

---

## ⚙️ Core Architecture and Capabilities

### 1. Connection Modes
*   **DoH Direct:** Resolves domains over binary RFC 8484 DNS-over-HTTPS via built-in secure providers:
    *   *Cloudflare* (`https://cloudflare-dns.com/dns-query`)
    *   *Google* (`https://dns.google/dns-query`)
    *   *Quad9* (`https://dns.quad9.net/dns-query`)
    *   *AdGuard* (`https://dns.adguard-dns.com/dns-query`)
*   **DoH Worker:** Redirects requests over customizable Cloudflare Workers proxy bypass endpoints to evade deep packet inspection (DPI) on restrictive routers.
*   **Psiphon Shield:** Bridges data channels securely through Psiphon Edge endpoints to evade firewall geo-restrictions.
*   **Psiphon + DoH Chaining:** Chains data proxying over Psiphon while routing secure DNS resolutions to direct or worker-side HTTPS endpoints.

### 2. DNS Wire Protocol Parser (`DnsWire`)
Fully implements RFC-compliant query binary serialization and parsing for direct A and AAAA records, enabling offline, serverless queries directly inside our Kotlin engine without system DNS pollution.

### 3. Local Proxy Sharing Server Ports
*   **HTTP Proxy:** Exposes an active socket listener on port `8080` supporting standard `HTTP 1.1 CONNECT` tunneling requests.
*   **SOCKS5 Proxy:** Exposes an active, lightweight, binary SOCKS5 socket thread on port `1080` (with `0x00 NO_AUTH` configuration).
*   **LAN Share:** Allows bridging ports from `127.0.0.1` onto the anycast LAN ip `0.0.0.0`, enabling nearby devices connected on the same Wi-Fi router to leverage the active secure VPN tunnel.

### 4. System VPN Tunneling (`VpnService`)
Interfaces directly with Android's virtual platform engine to establish a physical `VpnService` workspace:
*   Sets packet MTU to `1500`
*   Configures local virtual interface IP `10.0.0.2`
*   Applies anycast routing ranges (`0.0.0.0/0`)
*   **Only Apps Tunneling:** Selectively binds specific user applications using `builder.addAllowedApplication` so that only selected packages route traffic through our engine.
*   **Bypass Tunnelling / Apps Exclusion:** Excludes target application packages from the VPN interface using `builder.addDisallowedApplication`.

### 5. Vendored Deployed Core Adapters
Following stable engineering guidelines, **no external core downloads are performed at runtime**.
The release bundle vends pinned native binary maps for arm64-v8a inside target files, accessible via adapter triggers:
*   **XrayAdapter** (`/jniLibs/arm64-v8a/xray`)
*   **SingBoxAdapter** (`/jniLibs/arm64-v8a/sing-box`)
*   **PsiphonAdapter** (`/jniLibs/arm64-v8a/psiphon`)

---

## 🔒 Critical Architectural Limitations to Note

1.  **No Windows Executables:** Android compiles exclusively for Linux-based ARM/X86 architectures. Pinned files must match system ABIs (`arm64-v8a`, `armeabi-v7a`, `x86_64`) instead of Windows `.exe` formats.
2.  **User VPN Consent:** Establishing a tunnel requires system consent during activation. The OS displays standard security warnings; once agreed, the persistent key indicator is displayed in the notification bar.
3.  **Psiphon Sponsorship Config:** Connecting successfully onto Psiphon edges requires a valid server, sponsor, or provider config JSON. Custom endpoints can be added via the Modes & Profiles dialog.
4.  **No Deep Firewall Block without root:** Because non-rooted Android clients cannot directly alter system IP tables, blocking an app is handled by routing its output to the VPN loop and dropping its packets inside our adapter thread configuration.

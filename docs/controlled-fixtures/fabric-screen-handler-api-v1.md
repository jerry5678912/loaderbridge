# Fabric Screen Handler API v1 controlled fixture

This fixture tests the pinned
`fabric-screen-handler-api-v1:1.3.91+b559734419` contract through LoaderBridge
revision 1 on Minecraft 1.21.1 and Forge 52.1.16.

The bridge exposes all four pinned binary public classes:
`ExtendedScreenHandlerFactory`, `ExtendedScreenHandlerType`, its nested
`ExtendedFactory`, and `FabricScreenHandlerFactory`. The implementation uses
Forge's existing menu registry and LoaderBridge's Fabric Networking API bridge;
it does not install a second networking or screen system.

Content inspection finds API references in class descriptors and selects Screen
Handler, Networking API, and API Base automatically. The controlled Fabric mod
registers an `ExtendedScreenHandlerType` whose opening data contains a UTF-8
label and integer. A real server-player join opens the menu, its codec crosses
the payload channel, and the registered client factory constructs the screen.
The client rejects incorrect data and emits only after observing the exact
values:

`LOADERBRIDGE_FABRIC_SCREEN_HANDLER_READY label=loaderbridge-screen,value=37`

The marker appeared in both halves of the final graphical saved-world
open/reopen cycle. The actual
`FabricScreenHandlerClientFixture$FixtureScreen` was observed, the integrated
world saved all dimensions, reopened, and the client stopped cleanly. The same
final bridge JAR was side-safe in a dedicated-server saved-world run: Forge
reached ready, `save-all` completed, all dimensions saved, and shutdown exited
zero. Earlier fresh-world client and dedicated-server runs established the
creation path before the final warning-path-only review.

Unknown menu IDs, non-extended menu types, absent players, and missing screens
are logged and ignored as in the pinned Fabric implementation. Server-side
factory/type mismatches remain explicit errors rather than unexplained packet
or cast failures.

Pinned and generated evidence:

- bridge SHA-256: `c3f983dd5655d2dcdd7e0bf3f698e21607d9da33ae9c82ead8d00f6ddfd14e23`
- transformed fixture SHA-256: `cc7685f59af43e5aa9d75aaef15688512640bdb94d673dc4c8ca9f7bdf183234`
- lock SHA-256: `abab1d137198e33b127fba3ef7c31aea43244eee643c2fad393817c1d9b06f77`
- official binary JAR SHA-256: `314cfc816cead3ed07b7d9b40b208d7357f828bcb80dfea37564708d3d269075`
- official source JAR SHA-256: `5d0f74e4e13787bd410df3c415f030eade95cab8d53f89fd1e7c5858f05e9ecf`

Two independent preparations produced identical JAR names and SHA-256 values.
This closes the controlled Screen Handler API v1 M5 module gate. It does not
complete M5, prove Windows/Linux matrix parity, or establish the roadmap's 60%
catalog acceptance gate.

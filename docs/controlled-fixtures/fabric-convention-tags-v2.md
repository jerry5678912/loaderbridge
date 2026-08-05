# Fabric Convention Tags v2 controlled fixture

This fixture tests the pinned
`fabric-convention-tags-v2:2.12.0+c3656daa19` contract through LoaderBridge
revision 2 on Minecraft 1.21.1 and Forge 52.1.16.

The public ABI contains all nine pinned classes: `FabricTagKey`, `TagUtil`, and
the biome, block, item, fluid, entity-type, enchantment, and structure
convention classes. An automated binary comparison found exactly the same 79
biome, 118 block, 272 item, 12 fluid, 5 entity, 6 enchantment, and 2 structure
fields as the official artifact.

The bridge packages 493 standardized `c:` tag JSON resources and 14 translation
tables under the upstream Apache-2.0 license. These resources merge with
Forge's common tags. The runtime fixture proves real membership for End Stone,
an iron ingot, water, and the Ender Dragon. It also proves the Fabric plural
alias `c:tools/bows` contains a bow even though Forge 52's own pack does not
contain that alias file.

The early Mixin makes Minecraft's actual `TagKey` implement `FabricTagKey`.
The fixture called the injected method and observed
`tag.item.c.ingots.iron`. `TagUtil` static and registry-aware behavior is
covered by unit contracts and the live registry membership assertions.

The runtime marker was:

`LOADERBRIDGE_FABRIC_CONVENTION_TAGS_READY registries=5,alias=true`

The marker passed two dedicated-server JVMs loading the same saved world; both
reached ready, stopped cleanly, and saved all dimensions. The graphical client
observed it during both integrated-world resource loads, saved, returned to the
title screen, reopened the world, and stopped cleanly.

Prepared artifact evidence:

- bridge SHA-256: `e2767dc1b5df5339cd2beca7f7bb87effbbfc44d19992a9a306e78eb944d26f8`
- transformed fixture SHA-256: `e488a210fe3ac2688e9212081d59f2e5fd3ad1b08d6b882b02fa536a83e931db`
- lock SHA-256: `01f9aabd115aee7ae828cd930ee0deb2dc956ac0c5feae1a4b4424c6a602500f`
- pinned source JAR SHA-256: `0942c18987ea5be6c2e45a600e8b5b97483842ab961a21193cc851fea42ae20e`

Two independent preparations produced identical JAR digest lists. This closes
the controlled Convention Tags v2 M5 module gate. It does not complete M5,
prove Windows/Linux matrix parity, or establish the roadmap's 60% catalog gate.

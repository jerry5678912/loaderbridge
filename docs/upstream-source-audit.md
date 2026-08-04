# Upstream source audit

LoaderBridge uses upstream source to derive lifecycle behavior, then verifies
the decision against the exact pinned runtime and real-mod probes.

## User-supplied archives

- `fabric-loader-master.zip` identifies commit
  `b907c5b292fc062d75b6d8bf8255ac200109b992`. Its Minecraft `Hooks` invokes
  `main` before the active `client` or `server` entrypoint.
- `MinecraftForge-26.2.zip` identifies commit
  `d17cfd0b4bbfd192a9007f02240032f19b9b340d`. This is Forge for Minecraft
  26.2, not LoaderBridge's Minecraft 1.21.1 target, so it is architectural
  reference material rather than the target ABI authority.

The upstream repositories are
[Fabric Loader](https://github.com/FabricMC/fabric-loader) and
[MinecraftForge](https://github.com/MinecraftForge/MinecraftForge).

Fabric's supplied `EntrypointStorage` source records every metadata key before
normal initialization, creates modern entrypoints lazily, and caches a separate
instance for each requested contract type. LoaderBridge now follows those
observable contracts for both lifecycle and arbitrary API entrypoint keys.
The exact 0.16.14 `ModResolver` sorts its final unique selected-mod list by
canonical mod ID before `FabricLoaderImpl` registers containers and their
entrypoints. Forge 52.1.0 instead topologically sorts unrelated mod files using
their discovery index. LoaderBridge therefore sorts the public container and
entrypoint views by canonical Fabric ID, retaining metadata declaration order
within one provider and keeping aliases mapped to their canonical container.
The same pinned parser validates structured loading metadata before discovery:
entrypoints are an object of arrays, nested JARs and Mixins are arrays, language
adapters and dependency containers are objects, and their scalar values retain
strict JSON types. LoaderBridge now validates those shapes during untrusted-JAR
inspection and reports controlled `UnsafeJarException` messages rather than
leaking Gson shape casts. Like Fabric 0.16.14, unsupported scalar Mixin entries
are ignored while valid declarations in the same array remain active.
Fabric's `finishModLoading` adds every resolved non-builtin mod code-source path
to the target classpath before language adapters and entrypoints are set up.
LoaderBridge's transformed root and recursively extracted nested JARs share
Forge's game layer. A controlled parent now directly loads a non-entrypoint
class and resource present only in its nested child; the dedicated-server
scenario observed that marker in both launches around a world save and reload.
LoaderBridge now also attaches each queued lifecycle callback to its provider
ID and sorts the complete `preLaunch`, `main`, `client`, and dedicated-server
batches before invocation. This extends the pinned resolver's canonical order
from public API views to actual lifecycle execution, independent of Forge's
discovery order.
Fabric's `FabricMixinBootstrap` rejects a Mixin configuration resource name
when another active selected mod already declared it. LoaderBridge now checks
the side-filtered selected metadata graph and reports `LB-MIXIN-002` before its
generated namespaced wrappers could conceal that upstream collision.
Fabric's `CustomValueImpl` parses one immutable object/array tree, represents
numbers as `Double`, returns the stored nested value on repeated lookup, and
throws a type-specific `ClassCastException` instead of coercing values.
LoaderBridge now mirrors those identity, iteration, immutability, numeric, and
failure contracts rather than exposing Gson's conversions and exception types.
The metadata object also retains that one parsed declaration-ordered map;
repeated `getCustomValues()` and `getCustomValue()` calls now return the same
map and node identities instead of reparsing JSON at each access.
The pinned V1 metadata implementation likewise retains its author, contributor,
person-contact, and mod-contact objects. LoaderBridge now constructs those
immutable views once, returns Fabric's shared empty contact where applicable,
and preserves identity across repeated getters.
Fabric's semantic version implementation compares components through the longer
version, treating omitted components as zero. LoaderBridge now makes `1`,
`1.0`, and `1.0.0` equal as well as equally ordered, and normalizes their hashes
so equivalent dependency predicates deduplicate safely in collection APIs.
Fabric's dependency implementation also defines equality against any public
`ModDependency` implementation by kind, mod ID, and parsed predicates rather
than by its concrete implementation class. LoaderBridge mirrors that equality,
its matching hash, and Fabric's diagnostic string form so consumers can safely
compare bridge dependencies with their own implementations.
Fabric's `ModContainerImpl` retains ordinary object identity rather than value
equality and renders as its ID plus version. `ModOriginImpl` renders path roots
joined by the platform classpath separator or a nested origin as
`parent-id:sub-location`. LoaderBridge overrides its record defaults and
anonymous-origin defaults to preserve those observable diagnostics and
collection semantics.
Fabric's `getConfigDir()` creates the configured `config` directory before
returning it and wraps filesystem creation failures as a runtime error.
LoaderBridge now performs the same lazy creation so mods can immediately open
config files without a loader-specific directory bootstrap.
Fabric's `VersionIntervalImpl` compares any public interval implementation,
normalizes inclusivity at unbounded ends, renders mathematical interval bounds,
and treats plain-version point intervals differently from ordered semantic
intervals. LoaderBridge now preserves those value contracts, plain-version
intersection and union rules, complement edge cases, and deduplicates repeated
overlaps when intersecting interval collections.
Fabric's `ModPrioSorter` ranks root candidates before nested candidates and
newer versions before older versions, while its solver applies required version
predicates. LoaderBridge now makes the same generic decisions for duplicate
nested IDs in planning and preparation, iterates constraints contributed by the
selected candidates, and reports the chosen version. Multiple mandatory roots
with one ID remain a structured error. Hash deduplication also promotes an
artifact first seen nested when the identical JAR is subsequently installed as
a root. The preprocessing comparator now ignores semantic `+build` metadata
instead of falling back to lexical ordering.
Fabric's solver validates the complete selected combination, including a
candidate's own dependencies rather than only constraints placed on that
candidate by other mods. LoaderBridge now uses deterministic backtracking over
version alternatives and checks hard dependencies against candidate, alias,
Minecraft, Java, Loader, and implemented bridge-module versions. It also checks
identity collisions and `breaks`; an explicit search budget produces
`LB-NESTED-008` rather than an unbounded preprocessing run.
Fabric constrains every selected nested candidate to at least one selected
parent, while nested `IF_POSSIBLE` candidates are preferred but may remain
inactive when hard constraints contradict them. LoaderBridge now carries
content-hash parent edges and nesting depth into the solver, preserves every
parent when identical children are deduplicated, excludes children reachable
only through unselected parent variants, and normalizes shared-child runtime
origin metadata to a selected containing parent.
Capability planning now runs against that same selected candidate set rather
than only the user-installed root archives. Each selected nested archive is
extracted under the bounded preprocessing cache and statically analyzed without
class loading, so its API, namespace, native-library, MixinExtras, and language
adapter requirements cannot appear for the first time during preparation.
Fabric Loader installs its `default` adapter before processing mod-defined
adapters and fails when any later definition reuses an installed key.
LoaderBridge now performs the same global collision check over selected mods
before Forge construction and reports `LB-LANG-002` deterministically.
Fabric Loader completes this setup for every active mod before invoking the
`preLaunch` stage. LoaderBridge therefore queues prelaunch callbacks until
Forge's first `FMLConstructModEvent`; all container constructors and entrypoint
registrations have completed by then, while the later registry-dependent
`main` window remains unchanged.
The supplied public `FabricLoader` interface also exposed the previously missing
`getRawGameVersion()` binary contract; Forge runtime now obtains it from
`META-INF/loaderbridge.json`.
The pinned `0.16.14` Maven artifact is also resolved only on the test classpath.
An ASM contract test inventories all public types in `net.fabricmc.api`,
`net.fabricmc.loader.api`, and the public version-exception superclass, then
requires the shim JAR to preserve their JVM names, inheritance, static shape,
and member descriptors. This makes public API drift an automatic build failure;
the moving supplied `master` source remains behavioral reference material.
Fabric's `MappingResolverImpl` returns the launcher's actual target namespace.
Because Forge 52.1.0 executes LoaderBridge output in Mojang's official names,
the shim now reports `official`, exposes only the available `intermediary` and
`official` namespaces, and embeds that semantic header in new prepared JARs.
The older internal `named` header is accepted on input so existing prepared
caches remain usable, but it is never advertised as a Yarn mapping namespace.
Fabric's legacy `ModContainer.getPath` searches every active root before
falling back to a path under the first root, and returns a stable dummy path for
synthetic containers without roots. LoaderBridge mirrors this behavior so a
resource in the second root of a multi-project or multi-path mod is not silently
resolved to a nonexistent location in the first root.
Fabric normalizes mod-level and Mixin-level environment strings with
locale-independent lowercase rules, accepts universal, client, and server, and
rejects every other value. LoaderBridge now applies the same rule during
untrusted-JAR inspection instead of silently treating uppercase or malformed
environments as universal.
The supplied discovery implementation creates `java` from
`java.specification.version`, marks Java and Minecraft metadata as `builtin`,
and gives Minecraft a dependency on the Java class version it requires. Fabric
Loader itself is discovered from its ordinary `fabric.mod.json`, so its
metadata type remains `fabric`. LoaderBridge mirrors those observable
containers and uses one `0.16.14` compatibility constant for both dependency
planning and the runtime Loader container instead of allowing those phases to
advertise different versions.
Fabric's supplied Minecraft hooks pass the final game arguments through its
game provider, give the live `Minecraft` object to client initialization, and
deliberately pass null to dedicated-server initialization before publishing the
server object after its constructor. LoaderBridge mirrors those observable
timings: a structural transformer copies the argument array at the start of
each Minecraft `main(String[])`, the Forge client initialization boundary
publishes `Minecraft.getInstance()`, and `ServerAboutToStartEvent` publishes the
constructed dedicated server. No private ModLauncher fields are accessed.
Fabric's builtin Minecraft candidate retains every ordered `gameJars` path as
its container roots. Forge's `MinecraftLocator` builds its Minecraft mod from
the ordered paths returned by the public launch handler. LoaderBridge now feeds
that same Forge path list to the Fabric-compatible builtin container, preserving
multi-path layouts without trying to reconstruct them from filenames or module
internals.
Fabric's default language adapter recognizes `Class::<init>` alongside class,
field, and method entrypoints and adapts the selected executable to the requested
functional interface. Forge separates the bridge plugin and transformed mods
into named layers, so a direct lookup from the bridge module cannot always read
the target mod module. LoaderBridge preserves the typed method-handle behavior
with public reflection-backed handles, avoiding private module mutation while
supporting the complete standard member set.
Fabric Loader is itself discovered as a normal Fabric mod, so its public
container exposes the metadata and icon declared by its bundled
`fabric.mod.json`. LoaderBridge mirrors those published fields and includes the
Apache-licensed icon with explicit `NOTICE` attribution. Its root is derived
from the marker resource path rather than the protection-domain JAR path, which
keeps `findPath` valid in both ordinary directory tests and Forge's `union:`
virtual filesystem.

## Exact 1.21.1 authority

The implementation is compiled and tested against the pinned Maven artifacts
for Forge `1.21.1-52.1.0`, including its published source JAR. That exact source
shows:

- `FMLConstructModEvent` runs before Forge creates and populates its registries.
- Forge posts registry events before `FMLCommonSetupEvent`.
- `ModelEvent.RegisterAdditional` loads the model resource named by its model
  location directly, while vanilla item discovery loads `models/item/<id>` and
  stores it under the logical inventory model ID.
- `ItemRenderer` snapshots the item registry in its constructor and later
  rebuilds baked models from that item-to-model table.
- `ForgeHooksClient.initClientHooks` initializes `RecipeBookManager` while mod
  loading can still be proceeding, and `RegisterRecipeBookCategoriesEvent` is
  the last supported point for installing custom recipe-category finders.

Running all Fabric entrypoints during Forge construction was tested and
rejected: Oxidized reads Forge's `swim_speed` attribute while creating entity
attributes, but that holder is not bound until Forge's registry population.
LoaderBridge therefore retains a post-registry entrypoint window. On the
client, the first recipe-book registration event runs Fabric `main` and
`client`, captures recipe types they add, and registers generic finders on that
same event. Common setup remains the fallback and server path. The bridge also
repairs the client model discovery/cache boundary generically.

## Behavioral proof

The Oxidized probe now rejects missing block models, missing inventory models,
and missing textures before it performs its machine scenario. It then places
the kiln, processes clay and coal into a brick, saves, reloads, and verifies the
block, inventory item, and machine output. The separate custom recipe-book
check now passes: the run registers one finder for
`oxidized:kiln_smelting`, and Forge emits no unknown-category warning.

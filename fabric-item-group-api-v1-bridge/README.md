# Fabric Item Group API v1 bridge

This independently implements the public runtime contracts from
`fabric-item-group-api-v1:4.1.7+def88e3a19` for LoaderBridge's Forge 1.21.1
host. It maps custom creative-tab construction and keyed/global entry mutation
callbacks to Forge's native creative-tab content event.

The module is selected from inspected bytecode and Fabric metadata. It does not
contain or redistribute Fabric API implementation code.

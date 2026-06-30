## Changelog

# 1.20.1-1.9.1

- Fix broken slots interaction caused by refactoring
- Fix deleting items dropped into slots

# 1.20.1-1.9.0

- Add preset copy & paste function
- Refactor shared Redstone Link and Void Link menu/screen code

# 1.20.1-1.8.3

- Compat for Frequency Create by registering that mod's items in JEI and copying its menu
- Backend reconstruction
- Fix Create: Connected linked analog lever preview rendering
- Preserve signal, receiver/transmitter, and locked/powered state in non-void link previews while keeping preview orientation fixed
- Keep Void Link previews on the item-rendered path

# 1.20.1-1.8.2

- Hook Forge config translation keys for Configured

# 1.20.1-1.8.1

- Fix menu opening flow by moving link-click detection to the client and opening menus through a server packet
- Add client-side menu trigger mode config
- Add Void Link UI texture and frequency slot / ownership tooltips (made by 咖喱之恶魔)
- Fix held-item frequency setting on linked levers/buttons by using the actual clicked frequency slot

# 1.20.1-1.7.2

- Render Create: Connected linked lever/button previews

# 1.20.1-1.7.1

- Improve Void Motor relocation orientation

# 1.20.1-1.7.0

- Add 3D preview rendering for configured link blocks
- Add Void Link frequency GUI support for Create Utilities J 1.20.1-0.3.3
- Add Void Link owner claim/unclaim support

# 1.20.1-1.6.0

- Add actual texture to GUI, consistent with Create mod GUIs (made by 咖喱之恶魔)
- Improve indicator for moving links

# 1.20.1-1.5.0

- Port to Minecraft 1.20.1 and Forge 47.4.10
- Update Create dependency to 6.0.8-291
- Add runtime reference to Create: Connected 1.1.13
- Remove legacy aviation/sublevel compatibility code

# 1.21.1-1.5.0

- Preserve factory gauge connection upon movement
- 中文翻译
- fix badly implemented distance check

# 1.21.1-1.4.1

- Sable compatibility for the relocation feature

# 1.21.1-1.4.0

- Add button to relocate redstone link to new position, with configurable range
- Lower the needlessly high neoforge and create version requirement. Now work for all versions for minecraft 1.21.1

# 1.21.1-1.3.0

- Add indicator for red & blue frequency slots
- Add toggle switch for redstone link receive mode

# 1.21.1-1.2.0

- Now check for all blocks implementing "LinkBehavior", meaning compatibility with all blocks that uses link frequency.
- Now only open menu when clicking on frequency slot

# 1.21.1-1.1.1

- Optimizations
- Remove dead code
- Change network protocol versioning
- Changes to green highlight over frequency slots

# 1.21.1-1.1.0

- Adde EMI compatibility

# 1.21.1-1.0.0

First fully functional release

package com.asifulla.maya.agent

object ToolSchemas {
    // OpenAI Responses API function-tool schema. Keep this normalized: type/function/name/description/parameters.
    const val NATIVE_TOOLS = """
    [
      {"name":"open_app","description":"Open an installed Android application by package name","parameters":{"type":"object","properties":{"package_name":{"type":"string"}},"required":["package_name"]}},
      {"name":"click_text","description":"Tap a visible UI node by text","parameters":{"type":"object","properties":{"text":{"type":"string"},"exact":{"type":"boolean"}},"required":["text"]}},
      {"name":"click_view_id","description":"Tap a UI node by Android resource id","parameters":{"type":"object","properties":{"view_id":{"type":"string"}},"required":["view_id"]}},
      {"name":"click_description","description":"Tap a node by content description","parameters":{"type":"object","properties":{"description":{"type":"string"}},"required":["description"]}},
      {"name":"type_text","description":"Type into the focused editable node","parameters":{"type":"object","properties":{"text":{"type":"string"},"humanized":{"type":"boolean"}},"required":["text"]}},
      {"name":"tap","description":"Tap screen coordinates","parameters":{"type":"object","properties":{"x":{"type":"number"},"y":{"type":"number"}},"required":["x","y"]}},
      {"name":"swipe","description":"Swipe between screen coordinates","parameters":{"type":"object","properties":{"x1":{"type":"number"},"y1":{"type":"number"},"x2":{"type":"number"},"y2":{"type":"number"}},"required":["x1","y1","x2","y2"]}},
      {"name":"home","description":"Go Home","parameters":{"type":"object","properties":{}}},
      {"name":"back","description":"Navigate Back","parameters":{"type":"object","properties":{}}},
      {"name":"recents","description":"Open Recents","parameters":{"type":"object","properties":{}}},
      {"name":"notifications","description":"Open notification shade","parameters":{"type":"object","properties":{}}}
    ]
    """
}

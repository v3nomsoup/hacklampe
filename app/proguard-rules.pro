# HackLampe ProGuard/R8-Regeln.
# Die App nutzt keine Reflection; manifest-deklarierte Komponenten
# (Activity, Service, TileService, BroadcastReceiver) werden von R8
# automatisch behalten. Hier bewusst keine zusätzlichen Regeln nötig.

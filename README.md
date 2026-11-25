# Dificultad (Fabric 1.21.1)

Mod sin nada visual (sin HUD, mensajes, partículas ni sonidos propios). Solo el menú del palo.

## Palo de dificultad
Clic derecho (operadores, o en un mundo de un jugador) abre el menú:
1. **Iniciar cambios** / Detener: cada X segundos entra un cambio nuevo de los 20 (al azar o en fila). Los cambios se acumulan.
2. **Editar cambios**: activar/desactivar cada uno y cambiar su valor (x2, x3...). También el tiempo entre cambios.
3. **Comando antes de cada cambio**: se ejecuta justo antes de que entre cada cambio.
4. **Comando con temporizador**: cada vez que acaba el temporizador se ejecuta el comando (se repite).
5. **Eventos aleatorios**: 10 eventos que suben la dificultad (horda de zombis, creepers, tormenta de rayos, phantoms, hambre y debilidad, vexes, esqueletos wither, mobs fortalecidos, desgaste de armadura, noche forzada). Cada tanto se elige uno al azar.
6. **Zona que se cierra**: se activa donde estás. Cada X segundos se achica y baja vida poco a poco (dentro o fuera de la zona, a elegir).

Todo se guarda en `config/dificultad.json` y sigue después de reiniciar el servidor.

## Los 20 cambios
Zombis más daño, esqueletos disparan más rápido, creepers con explosión más grande, arañas rápidas, hostiles con más vida, más hostiles, endermans con más daño, no-muertos se queman más lento, zombis con armadura de hierro, flechas de mobs con más daño, más empuje, phantoms desde el día 1, mobs te detectan de más lejos, slimes más grandes, blazes con más bolas de fuego, ghasts con más vida, wardens con más daño, piglins hostiles en cualquier dimensión, más rayos en tormenta, hostiles atacan más rápido.

## Compilar
GitHub Actions compila solo al subir el proyecto: el .jar queda en **Actions → Artifacts** y en **Releases**.

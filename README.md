# Sistema de monitorización ciclista

Este repositorio contiene el codigo desarrollado para mi Trabajo de Fin de Grado, que consiste en un sistema de monitorización iclista que permite medir diversos parámetros relevantes de este deporte. El sistema está compuesto por un microcontrolador ESP32, que controla los sensores usados por el sistema, y por una aplicacion movil que recibe los datos del microcontrolador por BLE.

## Estructura
La raiz del proyecto dispone de dos directorios:
- MobileApp: Contiene el codigo de la aplicación móvil
- Projects: Contiene el codigo final del microcontrolador asi como otros proyectos relevantes que se han creado a lo largo del trabajo como pasos intermedios para el codigo final. Concretamente contiene los siguientes proyectos:
    - TFG: Es la versión final del código para el microcontrolador.
    - Mockup: Proyecto que simula los datos de los sensores y los manda por BLE, útil para probar la comunicación y la app sin necesidad de tener los sensores y la bicicleta.
    - Accelerometer Sensor: Código de prueba del MPU6050.
    - Barometer Sensor: Código de prueba del BME280.
    - Bluetooth Heart Rate Sensor: Código para probar la lectura de sensores de ritmo cardíaco por BLE.
    - Hall Sensor Test: Código de prueba del módulo KY-003.

## Requisitos

Antes de instalar y ejecutar el sistema, asegúrate de cumplir con los siguientes requisitos:

- **Dispositivo Android**: Recomendable Android 14 o superior. Puede funcionar con versiones a partir de Android 9 (aunque no fue probado).
- **Aplicación móvil**: Tener instalada la APK de la aplicación móvil o bien utilizar Android Studio o IntelliJ con las extensiones de Android para compilar el código e instalar la app.
- **Microcontrolador**: Contar con un microcontrolador ESP32 con el código en C++ cargado. Se puede cargar utilizando PlatformIO en VSCode o alternativas como Arduino IDE.
- **Sensores**:
  - Sensor de aceleración (MPU6050).
  - Sensor barométrico (BME280).
  - Sensores magnéticos para medir las revoluciones de la rueda y el pedalier (KY-003)
  - Sensor de frecuencia cardíaca compatible con BLE.

## Instalación del Sistema en la Bicicleta

1. **Conectar los sensores**: Sigue el diagrama de cableado proporcionado para conectar los sensores correctamente. Asegúrate de organizar los cables adecuadamente para evitar interferencias.
2. **Instalar los imanes**: Coloca un imán en la rueda y otro en el pedalier para medir las revoluciones.
3. **Fijar el microcontrolador**: Coloca el microcontrolador ESP32 en un lugar seguro y accesible de la bicicleta.
4. **Instalar los sensores**:
   - Coloca el sensor de aceleración (MPU6050) y el sensor barométrico (BME280).
   - Instala los sensores magnéticos: uno en la rueda y otro en el pedalier, ambos a la misma altura que sus respectivos imanes.
   - Si la bicicleta tiene suspensión delantera, asegúrate de dejar suficiente margen en los cables para evitar que se tensen.
5. **Fijar la batería**: Coloca la batería en un lugar seguro y accesible de la bicicleta.
6. **Verificación de los sensores**: Una vez instalados los sensores, realiza una prueba para asegurarte de que no rocen con los imanes o con otros componentes. Verifica que los sensores detecten correctamente los imanes.

## Inicio del Sistema

1. **Encender el sensor de frecuencia cardíaca**: Antes de conectar el microcontrolador a la alimentación, enciende primero el sensor de frecuencia cardíaca, ya que el sistema no funcionará correctamente sin él.
2. **Conectar el microcontrolador**: Conecta el microcontrolador ESP32 a la alimentación.
3. **Conectar con la aplicación móvil**:
   - Abre la aplicación móvil e inicia la búsqueda de dispositivos Bluetooth.
   - El microcontrolador debería aparecer como `ESP32_BIKE`.
4. **Iniciar actividad**:
   - Una vez conectado, accede a la vista de inicio de actividad en la aplicación.
   - Se te pedirá que ingreses la circunferencia de la rueda en milímetros y la altitud desde la que comienzas.
5. **Detener actividad**: Al finalizar la actividad, detén la sesión desde la aplicación móvil.
6. **Consultar actividad registrada**: Regresa al menú principal y selecciona la vista de actividades para ver la actividad que acabas de registrar.

# AstroCam
__Aplicacion para hacer fotometria con celulares__

Este proyecto esta basado en el repositorio [Camera2Raw](https://github.com/googlearchive/android-Camera2Raw)

## Requerimientos

- Un Telefono con Android 9 o superior
- Una camara con soporte para captura en 16-bit RAW
- Minimo 2 Gb de memoria RAM

## Detalles

Esta aplicacion permite la lectura directa de los datos del sensor de la camara, a diferencia de una fotografia convencional, los datos RAW son una cadena de bytes donde cada dos elementos representan un pixel del sensor.

Las camaras de los telefonos celulares tienen un Semiconductor complementario de óxido metálico o [CMOS](https://es.m.wikipedia.org/wiki/Semiconductor_complementario_de_%C3%B3xido_met%C3%A1lico) que a diferencia de un clasico sensor CCD los pixeles se organizan en un patron conocido como la [Matriz de Bayer](https://es.m.wikipedia.org/wiki/Mosaico_de_Bayer) donde el 50% de los pixeles son aquellos correpondientes al color verde, un 25% al color rojo y el otro 25% al azul. Esta aplicacion puede extraer la informacion correspondiente al canal verde de una imagen RAW y almacenarla en un fichero FITS.

El principal problema de los sensores CMOS en telefonos celulares es su capacidad limitada para recibir luz, es por eso que para tomar una imagen donde se puedan distinguir variaciones de brillo, esta aplicacion toma multiples imagenes y las combina usando una adicion simple (mas metodos se planean en el futuro), la imagen final se guarda como un fichero FITS en la memoria del telefono conteniendo los siguientes parametros:

Keyword		|		Descripcion
----------|-------------
DATE-OBS	|	Fecha de la captura
EXPTIME	|		Tiempo total de integracion
INTRUME		|			Modelo y Fabricante del telefono
STACK					|		Cantidad de imagenes apiladas
SENSOR_W/H	|	Tamaño del sensor en milimetros
SENSOR_I		|		Sensibilidad del sensor (ISO)

_Estos parametros pueden cambiar en el futuro_

## Uso

Para tomar una imagen, primero debe asegurarse que el telefono se encuentre quieto, apuntanto al cielo sobre un tripode o una mesa. Si el telefono detecta movimiento, la captura actual se cancelara.
Antes de iniciar una sesion de captura, hay que ir a configuracion y seleccionar el tiempo de integracion proramado, de esta forma el programa sabra cuantas imagenes se deben tomar para completar el tiempo de exposicion deseado, este numero de imagenes se vera afectado por el tiempo de exposicion maximo que proporciona el telefono.

Una vez tomada la imagen se oira un sonido de confirmacion, despues de eso es seguro levantar/mover el telefono, sim embargo no debe salir de la aplicacion ya que esta se encontrara haciendo el proceso de combinar y guardar el archivo final, cuando esto ocurra, una notificacion aparecera en la pantalla



*Jose Barria - Universidad de Valparaiso*

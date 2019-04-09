# MoodleTestSpringBoot

Conversor de formato propio a Moodle Gift realizado con Spring Boot.

**Ventajas:**
- Incrusta las imágenes en el fichero Gift que se importará al Moodle
- Realiza el cálculo automático de los %s de respuestas correctas e incorrectas

## Instrucciones
1. Ejecutar con: ```java -jar moodletest.jar``` y abrir navegador en http://localhost:8080
2. Crear un fichero .zip con el test llamado test.txt y todas las imágenes que contienen el test (no poner en ninguna subcarpeta).
3. En el Paso 1, subir el fichero que contiene el test y las imágenes (test.zip) a la web.
4. En el Paso 2, si se quiere generar un html con el tipo test y el solucionario rellenar el formulario de arriba. Sino, sólo indicar el % de resta de las respuestas incorrectas al final.
5. En el Paso 3, se puede descargar el fichero .gift para importar al Moodle, así como el .html del test y el .html del solucionario.

## Separación de preguntas
Al final de cada pregunta se añade una línea que contiene **-----** (también al final del test).

## Imágenes
Para incluir una imagen en el enunciado:
  - Guardar la imagen en una subcarpeta **img**
  - En el test, la línea comienza por **###** seguido del nombre del fichero de imagen. P.ej. ###imagen.png

## Respuestas
Las respuestas correctas se marcan al inicio con un **+** y las incorrectas con un **-**

## Preguntas fill in the blank
En el enunciado, indicar el blank con 5 guiones bajos y entre corchetes las diferentes soluciones posibles (separadas por #): **_____[Opcion1#Opcion2#Opcion3]**

## Preguntas matching
- Cada línea de la respuesta: *Respuesta visible->Opción correcta en desplegable
- Se pueden añadir opciones incorrectas en el desplegable: *->Opción incorrecta en desplegable

## Ejemplo de test de entrada

```
Consulte la ilustración. ¿Cuál es la dirección MAC de destino de la trama de Ethernet al salir del servidor web si el destino final es la PC1?
###i209881v1n2_209881.png
-00-60-2F-3A-07-AA
-00-60-2F-3A-07-BB
+00-60-2F-3A-07-CC
-00-60-2F-3A-07-DD
-----
¿Cuáles son los dos tipos de dispositivos a los que, en general, se asignan direcciones IP estáticas? (Elija dos opciones.)
-Estaciones de trabajo
+Servidores web
+Impresoras
-Hubs
-PC portátiles
-----
Complete el espacio en blanco.
El valor binario de ocho dígitos del último octeto de la dirección IPv4 172.17.10.7 es _____[00000111].
-----
Una cada descripción con una dirección IP adecuada. (No se utilizan todas las opciones.)
*Una dirección link-local->169.254.1.5
*Una dirección pública->198.133.219.2
*Una dirección experimental->240.2.6.255
*Una dirección de bucle invertido->127.0.0.1
*->172.18.45.9
-----
```

## Ejemplo de salida GIFT
```
[html]<pre style="font-family: PT Serif">
Consulte la ilustración. ¿Cuál es la dirección MAC de destino de la trama de Ethernet al salir del servidor web si el destino final es la PC1?
<img src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAmIAAAFjCAIAAAARgPCJAAAABmJLR0QAAAAAAAD5Q7t/AAAACXBIWXMAAA7EAAAOxAGVKw4bAAAgAElEQVR4nOydB3QTx9aA18amd5tOCiUkEEIIkBCSP+/lpScPElIfCYRminHDvVu992LJknsBA6Yb03vHhNBieu
 .....
 /FRXzaDpiDmAQAWxoeXVM6RTl6qsA5ICYBAABMQkwCAACYhJgEAAAwCTEJAABgEmISAADAJMQkAACASYhJAAAAkxCTAAAAJiEmAQAATEJMAgAAmISYBAAAMMm6mAQAAJiDEJMAAAAmmY9JAAAAGIOYBAAAMAkxCQAAYBJiEgAAwKT/Afcb9PhOxaFcAAAAAElFTkSuQmCC">
</pre>
{
    ~%-33.33333%00-60-2F-3A-07-AA
    ~%-33.33333%00-60-2F-3A-07-BB
    =00-60-2F-3A-07-CC
    ~%-33.33333%00-60-2F-3A-07-DD
}

[html]<pre style="font-family: PT Serif">
¿Cuáles son los dos tipos de dispositivos a los que, en general, se asignan direcciones IP estáticas? (Elija dos opciones.)
</pre>
{
    ~%-33.33333%Estaciones de trabajo
    ~%50%Servidores web
    ~%50%Impresoras
    ~%-33.33333%Hubs
    ~%-33.33333%PC portátiles
}

Complete el espacio en blanco.
El valor binario de ocho dígitos del último octeto de la dirección IPv4 172.17.10.7 es {=00000111}.

[html]<pre style="font-family: PT Serif">
Una cada descripción con una dirección IP adecuada. (No se utilizan todas las opciones.)
</pre>
{
    =Una dirección link-local -> 169.254.1.5
    =Una dirección pública -> 198.133.219.2
    =Una dirección experimental -> 240.2.6.255
    =Una dirección de bucle invertido -> 127.0.0.1
    = -> 172.18.45.9
}
```

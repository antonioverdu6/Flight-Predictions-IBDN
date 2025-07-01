# Usa una imagen base de Python
FROM python:3.10-slim

# Crear directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiar el archivo requirements.txt primero para aprovechar el cache de Docker
COPY requirements.txt .

# Instalar todas las dependencias listadas en requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# Copiar el resto de tu código de aplicación Flask
COPY . .

# Exponer el puerto que usa Flask (asegúrate de que tu app Flask escuche en este puerto)
EXPOSE 5001

# Iniciar la aplicación Flask
CMD ["python", "predict_flask.py"]

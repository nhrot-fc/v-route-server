#!/bin/bash

# Script para ejecutar el experimento de algoritmos de optimización de rutas
# Este script compila y ejecuta la aplicación AlgorithmExperiment

# Colores para mensajes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}🚀 EXPERIMENTO DE ALGORITMOS DE OPTIMIZACIÓN DE RUTAS 🚀${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"
echo

# Verificar que Java está instalado
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java no está instalado. Por favor, instale Java para continuar.${NC}"
    exit 1
fi

# Verificar que Gradle está instalado
if ! command -v ./gradlew &> /dev/null; then
    echo -e "${RED}❌ Gradle Wrapper no encontrado. Asegúrese de estar en el directorio raíz del proyecto.${NC}"
    exit 1
fi

echo -e "${GREEN}📦 Compilando el proyecto...${NC}"
./gradlew clean build -x test || {
    echo -e "${RED}❌ Error al compilar el proyecto.${NC}"
    exit 1
}

echo -e "\n${GREEN}🚀 Ejecutando experimento...${NC}"
echo -e "${BLUE}Los resultados se guardarán en el directorio experiment_results/${NC}"
echo -e "\n${BLUE}Esto puede tomar varios minutos dependiendo de la configuración...${NC}\n"

# Ejecutar usando Spring Boot con perfil específico
./gradlew bootRun --args="--spring.profiles.active=algorithm-experiment"

echo -e "\n${GREEN}🔍 Los resultados están disponibles en el directorio experiment_results/${NC}"

echo -e "\n${BLUE}════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✅ Experimento finalizado.${NC}"
echo -e "${BLUE}════════════════════════════════════════════════════════════${NC}"

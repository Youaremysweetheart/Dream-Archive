#!/bin/bash

# 梦境档案馆系统 - 快速启动脚本

echo "🌙 梦境档案馆系统 - 启动脚本"
echo "================================"

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 检查Java
echo -e "${YELLOW}检查Java环境...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}错误: 未找到Java，请先安装JDK 17或更高版本${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Java版本: $(java -version 2>&1 | head -n 1)${NC}"

# 检查Maven
echo -e "${YELLOW}检查Maven环境...${NC}"
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}错误: 未找到Maven，请先安装Maven${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Maven版本: $(mvn -version | head -n 1)${NC}"

# 检查MySQL
echo -e "${YELLOW}检查MySQL服务...${NC}"
if ! command -v mysql &> /dev/null; then
    echo -e "${RED}警告: 未找到MySQL客户端${NC}"
else
    echo -e "${GREEN}✓ MySQL已安装${NC}"
fi

# 检查Node.js
echo -e "${YELLOW}检查Node.js环境...${NC}"
if ! command -v node &> /dev/null; then
    echo -e "${RED}错误: 未找到Node.js，请先安装Node.js${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Node.js版本: $(node --version)${NC}"

echo ""
echo "================================"
echo "请选择操作:"
echo "1. 初始化数据库"
echo "2. 启动后端服务"
echo "3. 启动前端服务"
echo "4. 同时启动前后端"
echo "5. 构建生产版本"
echo "0. 退出"
echo "================================"

read -p "请输入选项 (0-5): " choice

case $choice in
    1)
        echo -e "${YELLOW}初始化数据库...${NC}"
        read -p "请输入MySQL用户名 [root]: " mysql_user
        mysql_user=${mysql_user:-root}
        read -s -p "请输入MySQL密码: " mysql_pass
        echo ""
        
        mysql -u$mysql_user -p$mysql_pass < backend/init.sql
        
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ 数据库初始化成功！${NC}"
        else
            echo -e "${RED}✗ 数据库初始化失败${NC}"
        fi
        ;;
        
    2)
        echo -e "${YELLOW}启动后端服务...${NC}"
        cd backend
        mvn spring-boot:run
        ;;
        
    3)
        echo -e "${YELLOW}启动前端服务...${NC}"
        cd frontend
        
        if [ ! -d "node_modules" ]; then
            echo -e "${YELLOW}首次运行，正在安装依赖...${NC}"
            npm install
        fi
        
        npm run dev
        ;;
        
    4)
        echo -e "${YELLOW}同时启动前后端...${NC}"
        
        # 启动后端
        echo -e "${YELLOW}正在启动后端...${NC}"
        cd backend
        mvn spring-boot:run > ../backend.log 2>&1 &
        BACKEND_PID=$!
        cd ..
        
        sleep 5
        
        # 启动前端
        echo -e "${YELLOW}正在启动前端...${NC}"
        cd frontend
        
        if [ ! -d "node_modules" ]; then
            echo -e "${YELLOW}首次运行，正在安装依赖...${NC}"
            npm install
        fi
        
        npm run dev
        
        # 清理
        kill $BACKEND_PID 2>/dev/null
        ;;
        
    5)
        echo -e "${YELLOW}构建生产版本...${NC}"
        
        # 构建后端
        echo -e "${YELLOW}构建后端...${NC}"
        cd backend
        mvn clean package -DskipTests
        
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ 后端构建成功！${NC}"
            echo -e "JAR文件: backend/target/dream-archive-backend-1.0.0.jar"
        else
            echo -e "${RED}✗ 后端构建失败${NC}"
            exit 1
        fi
        
        cd ..
        
        # 构建前端
        echo -e "${YELLOW}构建前端...${NC}"
        cd frontend
        
        if [ ! -d "node_modules" ]; then
            npm install
        fi
        
        npm run build
        
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ 前端构建成功！${NC}"
            echo -e "静态文件: frontend/dist/"
        else
            echo -e "${RED}✗ 前端构建失败${NC}"
            exit 1
        fi
        
        cd ..
        
        echo ""
        echo -e "${GREEN}================================${NC}"
        echo -e "${GREEN}构建完成！${NC}"
        echo -e "后端JAR: backend/target/dream-archive-backend-1.0.0.jar"
        echo -e "前端静态文件: frontend/dist/"
        echo -e "${GREEN}================================${NC}"
        ;;
        
    0)
        echo "退出"
        exit 0
        ;;
        
    *)
        echo -e "${RED}无效的选项${NC}"
        exit 1
        ;;
esac

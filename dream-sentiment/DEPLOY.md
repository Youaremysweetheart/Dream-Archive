# 部署文档 — 阿里云 ECS (2v4G, Ubuntu 22.04)

---

## 1. 上传项目到服务器

```bash
# 本地打包（在 dream-sentiment 上级目录执行）
tar -czf dream-sentiment.tar.gz dream-sentiment --exclude=venv --exclude=__pycache__

# 上传到服务器
scp dream-sentiment.tar.gz ubuntu@你的服务器IP:/home/ubuntu/

# SSH 登录服务器
ssh ubuntu@你的服务器IP
```

或直接在服务器上 git clone 项目后单独上传 `dream-sentiment/` 目录。

---

## 2. 安装系统依赖

```bash
sudo apt update && sudo apt upgrade -y

# Python 3.10 + pip + venv
sudo apt install -y python3 python3-pip python3-venv

# jieba 分词需要的编译依赖
sudo apt install -y build-essential
```

---

## 3. 创建虚拟环境 & 安装 Python 依赖

```bash
cd /home/ubuntu/dream-sentiment
tar -xzf ../dream-sentiment.tar.gz
cd dream-sentiment

# 创建虚拟环境
python3 -m venv venv
source venv/bin/activate

# 安装依赖（CPU 版 PyTorch，服务器不需要 GPU）
pip install --upgrade pip
pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cpu
pip install flask flask-cors jieba transformers scikit-learn tqdm safetensors
```

---

## 4. 上传训练好的模型文件

模型文件（`checkpoints/` 目录）需要在本地训练好后上传到服务器：

```bash
# 在本地打包 checkpoints
cd D:\111work\Projects\Dream-Archive-sys\dream-sentiment
tar -czf checkpoints.tar.gz checkpoints/

# 上传到服务器
scp checkpoints.tar.gz ubuntu@你的服务器IP:/home/ubuntu/dream-sentiment/

# 服务器上解压
ssh ubuntu@你的服务器IP
cd /home/ubuntu/dream-sentiment
tar -xzf checkpoints.tar.gz
```

---

## 5. 测试运行

```bash
cd /home/ubuntu/dream-sentiment
source venv/bin/activate
python app.py
```

访问 `http://服务器IP:5000/health` 确认服务正常。确认后按 Ctrl+C 停掉，继续下一步。

---

## 6. 配置 systemd 服务（开机自启 + 进程守护）

```bash
sudo vim /etc/systemd/system/dream-sentiment.service
```

写入：

```ini
[Unit]
Description=Dream Sentiment Analysis Service
After=network.target

[Service]
User=ubuntu
WorkingDirectory=/home/ubuntu/dream-sentiment
Environment="PATH=/home/ubuntu/dream-sentiment/venv/bin"
ExecStart=/home/ubuntu/dream-sentiment/venv/bin/gunicorn -w 4 -b 0.0.0.0:5000 app:app
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

安装 gunicorn：

```bash
source venv/bin/activate
pip install gunicorn
```

启动服务：

```bash
sudo systemctl daemon-reload
sudo systemctl enable dream-sentiment
sudo systemctl start dream-sentiment
sudo systemctl status dream-sentiment  # 查看状态
```

---

## 7. 配置 Nginx 反向代理（可选，推荐）

```bash
sudo apt install -y nginx
sudo vim /etc/nginx/sites-available/dream-sentiment
```

写入：

```nginx
server {
    listen 80;
    server_name 你的域名或IP;

    location /sentiment/ {
        rewrite ^/sentiment/(.*)$ /$1 break;
        proxy_pass http://127.0.0.1:5000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location / {
        # 这里指向你的 Vue 前端
        root /home/ubuntu/frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
}
```

启用：

```bash
sudo ln -s /etc/nginx/sites-available/dream-sentiment /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl restart nginx
```

---

## 8. 防火墙开放端口

```bash
# 如果直接用 5000 端口
sudo ufw allow 5000

# 如果用 Nginx 反代则开 80 端口
sudo ufw allow 80
sudo ufw allow 443  # HTTPS 用

# 阿里云 ECS 还需要在控制台→安全组→入方向开放对应端口
```

---

## 9. 前端调用地址

根据 Nginx 配置，前端访问地址为：

```javascript
// Vue 中调用
const API_BASE = "http://你的服务器IP/sentiment"
// 或
const API_BASE = "http://你的域名/sentiment"

fetch(`${API_BASE}/analyze`, {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({ text: "梦见...", model: "cnn" })
})
```

---

## 常用命令速查

```bash
# 查看服务状态
sudo systemctl status dream-sentiment

# 查看实时日志
sudo journalctl -u dream-sentiment -f

# 重启服务
sudo systemctl restart dream-sentiment

# 停止服务
sudo systemctl stop dream-sentiment

# 只测试不通过 systemd
cd /home/ubuntu/dream-sentiment && source venv/bin/activate && python app.py
```

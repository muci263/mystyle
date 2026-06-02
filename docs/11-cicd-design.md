# 智能个人作品集系统 - CI/CD 与部署设计文档

## 1. 目标

实现从代码提交到服务器部署的自动化流程，使本项目具备真实上线能力，并能在面试中展示完整工程链路。

## 2. 环境规划

```text
local       本地开发
staging     预发布，可后续补充
production  生产环境
```

第一期先实现：

- local
- production

## 3. 仓库结构建议

```text
mystyle
  apps
    web
    server
  infra
    docker
    nginx
    jenkins
  docs
```

## 4. Docker 服务

```text
web       Next.js 前端
server    Spring Boot 后端
mysql     MySQL
redis     Redis
nginx     反向代理
```

## 5. Nginx 转发规则

```text
/           -> web:3000
/api        -> server:8080
/swagger    -> server:8080
```

生产环境建议开启 HTTPS。

## 6. Jenkins 流水线设计

触发方式：

- GitHub Webhook
- 手动构建

流水线步骤：

```text
Checkout
  -> Install Frontend Dependencies
  -> Frontend Lint
  -> Frontend Build
  -> Backend Test
  -> Backend Package
  -> Build Docker Images
  -> Deploy Docker Compose
  -> Health Check
  -> Notify Result
```

## 7. Jenkinsfile 草案

```groovy
pipeline {
  agent any

  environment {
    APP_NAME = 'mystyle'
    DEPLOY_DIR = '/opt/mystyle'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Frontend Build') {
      steps {
        dir('apps/web') {
          sh 'npm ci'
          sh 'npm run lint'
          sh 'npm run build'
        }
      }
    }

    stage('Backend Test and Package') {
      steps {
        dir('apps/server') {
          sh './mvnw test'
          sh './mvnw package -DskipTests'
        }
      }
    }

    stage('Docker Build') {
      steps {
        sh 'docker compose -f infra/docker/docker-compose.prod.yml build'
      }
    }

    stage('Deploy') {
      steps {
        sh 'docker compose -f infra/docker/docker-compose.prod.yml up -d'
      }
    }

    stage('Health Check') {
      steps {
        sh 'curl -f http://localhost/api/health'
      }
    }
  }
}
```

实际开发时需要根据服务器路径、权限、镜像仓库和环境变量调整。

## 8. 环境变量

需要提供 `.env.example`，包含：

```text
MYSQL_HOST=
MYSQL_PORT=
MYSQL_DATABASE=
MYSQL_USERNAME=
MYSQL_PASSWORD=

REDIS_HOST=
REDIS_PORT=
REDIS_PASSWORD=

JWT_SECRET=
JWT_EXPIRES_IN=

LLM_PROVIDER=
LLM_API_KEY=
LLM_BASE_URL=
LLM_MODEL=

NEXT_PUBLIC_API_BASE_URL=
```

真实 `.env` 不提交仓库。

## 9. 部署策略

第一期：

- Docker Compose 原地更新。
- 健康检查失败后人工处理。

第二期：

- 增加备份脚本。
- 增加回滚脚本。
- 增加镜像版本号。
- 部署失败保留上一版容器。

## 10. 健康检查

后端：

```http
GET /api/health
```

返回：

```json
{
  "status": "UP",
  "mysql": "UP",
  "redis": "UP"
}
```

前端：

- 首页可访问。

Nginx：

- `/api/health` 可转发成功。

## 11. 数据库备份

第一期手动备份：

```text
mysqldump
```

后续增强：

- 每日定时备份。
- 保留最近 7 天。
- 部署前自动备份。

## 12. 安全注意事项

- Jenkins 凭据使用 Credentials 管理。
- 服务器 `.env` 权限限制。
- 不在日志中打印 LLM API Key、数据库密码、JWT Secret。
- 后台管理路径必须鉴权。
- 生产环境关闭详细错误堆栈。

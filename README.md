# LambdaCraft

**(λ () (study math) (practice code))**

## Deploy

Publish the site to server `i`:

```bash
DEPLOY_SERVER=i DEPLOY_DIR=/root/repos/lambdacraft ./scripts/deploy.sh
```

The deploy script syncs the local source tree to the remote directory, then runs `docker compose up -d --build` on the server.

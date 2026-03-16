FROM node:20-alpine
WORKDIR /app

ENV NODE_ENV=production

COPY package*.json ./
RUN npm install

COPY tsconfig.json ./
COPY src ./src

RUN mkdir -p /app/uploads/logos

EXPOSE 8080

CMD ["npx", "tsx", "src/index.ts"]

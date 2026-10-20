# ---------- Build stage ----------
FROM node:20-alpine AS builder
WORKDIR /app

# Install dependencies (deterministic)
COPY package*.json ./
RUN npm ci

# Copy source files
COPY . .

# Build production bundle
RUN npm run build

# ---------- Runtime stage ----------
FROM nginx:alpine
# Remove default config
RUN rm /etc/nginx/conf.d/default.conf
# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/
# Copy built assets
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

import dotenv from "dotenv";
import { z } from "zod";

dotenv.config();

const envSchema = z.object({
  MYSQL_HOST: z.string().min(1, "MYSQL_HOST is required."),
  MYSQL_PORT: z.coerce.number().int().positive(),
  MYSQL_USER: z.string().min(1, "MYSQL_USER is required."),
  MYSQL_PASSWORD: z.string().min(1, "MYSQL_PASSWORD is required."),
  MYSQL_DATABASE: z.string().min(1, "MYSQL_DATABASE is required."),
  PORT: z.coerce.number().default(4000),
  CLIENT_ORIGIN: z.string().default("http://localhost:5173")
});

export const env = envSchema.parse(process.env);

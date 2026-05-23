import { readFile } from "node:fs/promises";

const googleServices = JSON.parse(
  await readFile(new URL("../app/google-services.json", import.meta.url), "utf8")
);

const projectId = googleServices.project_info.project_id;
const apiKey = googleServices.client[0].api_key[0].current_key;
const firestoreBase =
  `https://firestore.googleapis.com/v1/projects/${projectId}/databases/(default)/documents`;

const password = "123456";

const accounts = [
  {
    email: "fer1@test.com",
    name: "Fernando",
    bio: "Cocina casera, tortillas jugosas y cenas rápidas.",
  },
  {
    email: "fer2@test.com",
    name: "Alejandro",
    bio: "Fan de la pasta, los guisos lentos y las salsas con carácter.",
  },
  {
    email: "test1@sazon.com",
    name: "Marta",
    bio: "Postres sencillos y recetas para compartir.",
  },
];

const now = Date.now();
const hours = (n) => n * 60 * 60 * 1000;
const days = (n) => n * 24 * 60 * 60 * 1000;

const recipeTemplates = [
  {
    id: "demo-tortilla-jugosa",
    ownerEmail: "fer1@test.com",
    title: "Tortilla de patatas jugosa",
    description:
      "Patata pochada despacio, huevo bien batido y reposo corto antes de cuajar. El truco está en sacarla cuando el centro aún tiembla un poco.",
    imageUrl:
      "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?auto=format&fit=crop&w=900&q=80",
    likes: 8,
    createdAt: now - hours(5),
  },
  {
    id: "demo-pasta-setas",
    ownerEmail: "fer2@test.com",
    title: "Pasta cremosa con setas",
    description:
      "Saltea setas con ajo, añade nata ligera, pimienta negra y termina la pasta en la salsa con un poco del agua de cocción.",
    imageUrl:
      "https://images.unsplash.com/photo-1473093295043-cdd812d0e601?auto=format&fit=crop&w=900&q=80",
    likes: 11,
    createdAt: now - days(1),
  },
  {
    id: "demo-gazpacho-verano",
    ownerEmail: "fer2@test.com",
    title: "Gazpacho suave de verano",
    description:
      "Tomate maduro, pepino, pimiento, pan, aceite de oliva y vinagre. Enfriar bien y servir con picatostes.",
    imageUrl:
      "https://images.unsplash.com/photo-1625944525533-473f1a3d54e7?auto=format&fit=crop&w=900&q=80",
    likes: 6,
    createdAt: now - days(2),
  },
  {
    id: "demo-bizcocho-limon",
    ownerEmail: "test1@sazon.com",
    title: "Bizcocho de limón esponjoso",
    description:
      "Yogur, ralladura de limón y aceite suave. Horno medio y paciencia: no abrir la puerta hasta que suba por completo.",
    imageUrl:
      "https://images.unsplash.com/photo-1519869325930-281384150729?auto=format&fit=crop&w=900&q=80",
    likes: 14,
    createdAt: now - days(3),
  },
];

const comments = [
  {
    id: "demo-comment-fer2-tortilla",
    recipeId: "demo-tortilla-jugosa",
    authorEmail: "fer2@test.com",
    text: "La probé así y queda muchísimo más cremosa. Buen truco.",
    createdAt: now - hours(3),
  },
  {
    id: "demo-comment-test1-tortilla",
    recipeId: "demo-tortilla-jugosa",
    authorEmail: "test1@sazon.com",
    text: "Con cebolla caramelizada tiene que quedar increíble.",
    createdAt: now - hours(2),
  },
  {
    id: "demo-comment-fer1-pasta",
    recipeId: "demo-pasta-setas",
    authorEmail: "fer1@test.com",
    text: "Me guardo esta para una cena rápida entre semana.",
    createdAt: now - hours(20),
  },
];

const likes = [
  ["demo-tortilla-jugosa", "fer2@test.com"],
  ["demo-tortilla-jugosa", "test1@sazon.com"],
  ["demo-pasta-setas", "fer1@test.com"],
  ["demo-pasta-setas", "test1@sazon.com"],
  ["demo-bizcocho-limon", "fer1@test.com"],
  ["demo-bizcocho-limon", "fer2@test.com"],
];

const saved = [
  ["fer1@test.com", "demo-pasta-setas"],
  ["fer1@test.com", "demo-bizcocho-limon"],
  ["fer2@test.com", "demo-tortilla-jugosa"],
  ["test1@sazon.com", "demo-tortilla-jugosa"],
];

const follows = [
  ["fer1@test.com", "fer2@test.com"],
  ["fer1@test.com", "test1@sazon.com"],
  ["fer2@test.com", "fer1@test.com"],
  ["test1@sazon.com", "fer1@test.com"],
];

const demoUsers = new Map();

async function main() {
  console.log(`Seeding Firebase project ${projectId}`);

  for (const account of accounts) {
    const auth = await ensureAuthUser(account);
    demoUsers.set(account.email, { ...account, ...auth });
  }

  await seedUserProfiles();
  await seedRecipes();
  await seedLikes();
  await seedSavedRecipes();
  await seedFollows();
  await seedComments();
  await seedChat();

  console.log("Demo seed complete.");
  console.log("Accounts:");
  for (const account of accounts) {
    console.log(`- ${account.email} / ${password}`);
  }
}

async function ensureAuthUser(account) {
  const signUp = await identity("accounts:signUp", {
    email: account.email,
    password,
    displayName: account.name,
    returnSecureToken: true,
  });

  if (signUp.ok) {
    return {
      uid: signUp.data.localId,
      idToken: signUp.data.idToken,
      refreshToken: signUp.data.refreshToken,
    };
  }

  const message = signUp.data?.error?.message || "";
  if (!message.includes("EMAIL_EXISTS")) {
    throw new Error(`No se pudo crear ${account.email}: ${message}`);
  }

  const login = await identity("accounts:signInWithPassword", {
    email: account.email,
    password,
    returnSecureToken: true,
  });
  if (!login.ok) {
    throw new Error(
      `No se pudo iniciar sesión con ${account.email}: ${login.data?.error?.message}`
    );
  }

  await identity("accounts:update", {
    idToken: login.data.idToken,
    displayName: account.name,
    returnSecureToken: false,
  });

  return {
    uid: login.data.localId,
    idToken: login.data.idToken,
    refreshToken: login.data.refreshToken,
  };
}

async function seedUserProfiles() {
  const followerCount = countSecond(follows);
  const followingCount = countFirst(follows);
  const recipeCount = countBy(recipeTemplates, (recipe) => recipe.ownerEmail);

  for (const account of accounts) {
    const user = demoUsers.get(account.email);
    await setDoc(
      `users/${user.uid}`,
      {
        uid: user.uid,
        name: account.name,
        email: account.email,
        bio: account.bio,
        avatarUrl: "",
        followers: followerCount.get(account.email) || 0,
        following: followingCount.get(account.email) || 0,
        recipes: recipeCount.get(account.email) || 0,
        createdAt: now - days(30),
      },
      user.idToken
    );
  }
}

async function seedRecipes() {
  for (const recipe of recipeTemplates) {
    const owner = demoUsers.get(recipe.ownerEmail);
    await setDoc(
      `recipes/${recipe.id}`,
      {
        authorId: owner.uid,
        autor: owner.name,
        titulo: recipe.title,
        descripcion: recipe.description,
        imageUrl: recipe.imageUrl,
        likes: recipe.likes,
        createdAt: recipe.createdAt,
        updatedAt: recipe.createdAt,
      },
      owner.idToken
    );
  }
}

async function seedLikes() {
  for (const [recipeId, email] of likes) {
    const user = demoUsers.get(email);
    await createIfMissing(
      `recipes/${recipeId}/likes/${user.uid}`,
      {
        uid: user.uid,
        createdAt: now - hours(4),
      },
      user.idToken
    );
  }
}

async function seedSavedRecipes() {
  for (const [email, recipeId] of saved) {
    const user = demoUsers.get(email);
    await setDoc(
      `users/${user.uid}/saved/${recipeId}`,
      {
        recipeId,
        createdAt: now - hours(2),
      },
      user.idToken
    );
  }
}

async function seedFollows() {
  for (const [fromEmail, toEmail] of follows) {
    const from = demoUsers.get(fromEmail);
    const to = demoUsers.get(toEmail);
    const createdAt = now - days(2);
    await createIfMissing(
      `users/${from.uid}/following/${to.uid}`,
      { createdAt },
      from.idToken
    );
    await createIfMissing(
      `users/${to.uid}/followers/${from.uid}`,
      { createdAt },
      from.idToken
    );
  }
}

async function seedComments() {
  for (const comment of comments) {
    const author = demoUsers.get(comment.authorEmail);
    await createIfMissing(
      `recipes/${comment.recipeId}/comments/${comment.id}`,
      {
        recipeId: comment.recipeId,
        authorId: author.uid,
        authorName: author.name,
        text: comment.text,
        createdAt: comment.createdAt,
      },
      author.idToken
    );
  }
}

async function seedChat() {
  const fernando = demoUsers.get("fer1@test.com");
  const alejandro = demoUsers.get("fer2@test.com");
  const chatId = buildChatId(fernando.uid, alejandro.uid);
  const messages = [
    {
      id: "demo-msg-1",
      sender: fernando,
      text: "He publicado la tortilla para la demo.",
      at: now - minutes(18),
    },
    {
      id: "demo-msg-2",
      sender: alejandro,
      text: "Perfecto, yo enseño la pasta y probamos comentarios.",
      at: now - minutes(15),
    },
    {
      id: "demo-msg-3",
      sender: fernando,
      text: "Luego abrimos seguidores y cerramos con el chat en tiempo real.",
      at: now - minutes(12),
    },
  ];

  await setDoc(
    `chats/${chatId}`,
    {
      participants: [fernando.uid, alejandro.uid],
      participantsNames: {
        [fernando.uid]: fernando.name,
        [alejandro.uid]: alejandro.name,
      },
      lastMessage: messages.at(-1).text,
      lastSenderId: messages.at(-1).sender.uid,
      lastMessageAt: new Date(messages.at(-1).at),
      lastReadAt: {
        [fernando.uid]: new Date(now - minutes(10)),
        [alejandro.uid]: new Date(now - minutes(14)),
      },
      presence: {
        [fernando.uid]: {
          active: false,
          typing: false,
          lastSeen: new Date(now - minutes(10)),
        },
        [alejandro.uid]: {
          active: false,
          typing: false,
          lastSeen: new Date(now - minutes(14)),
        },
      },
    },
    fernando.idToken
  );

  for (const message of messages) {
    await createIfMissing(
      `chats/${chatId}/messages/${message.id}`,
      {
        text: message.text,
        senderId: message.sender.uid,
        createdAt: new Date(message.at),
        readBy: [fernando.uid, alejandro.uid],
      },
      message.sender.idToken
    );
  }
}

async function identity(method, body) {
  const response = await fetch(
    `https://identitytoolkit.googleapis.com/v1/${method}?key=${apiKey}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }
  );
  return { ok: response.ok, data: await response.json() };
}

async function docExists(path, idToken) {
  const response = await fetch(`${firestoreBase}/${path}`, {
    headers: { Authorization: `Bearer ${idToken}` },
  });
  if (response.status === 404) return false;
  if (!response.ok) {
    throw new Error(`Error leyendo ${path}: ${await response.text()}`);
  }
  return true;
}

async function createIfMissing(path, data, idToken) {
  if (await docExists(path, idToken)) return;
  await setDoc(path, data, idToken);
}

async function setDoc(path, data, idToken) {
  const response = await fetch(`${firestoreBase}/${path}`, {
    method: "PATCH",
    headers: {
      Authorization: `Bearer ${idToken}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ fields: toFields(data) }),
  });
  if (!response.ok) {
    throw new Error(`Error escribiendo ${path}: ${await response.text()}`);
  }
}

function toFields(data) {
  return Object.fromEntries(
    Object.entries(data)
      .filter(([, value]) => value !== undefined)
      .map(([key, value]) => [key, toFirestoreValue(value)])
  );
}

function toFirestoreValue(value) {
  if (value === null) return { nullValue: null };
  if (value instanceof Date) return { timestampValue: value.toISOString() };
  if (Array.isArray(value)) {
    return { arrayValue: { values: value.map(toFirestoreValue) } };
  }
  if (typeof value === "object") {
    return { mapValue: { fields: toFields(value) } };
  }
  if (typeof value === "string") return { stringValue: value };
  if (typeof value === "boolean") return { booleanValue: value };
  if (Number.isInteger(value)) return { integerValue: String(value) };
  if (typeof value === "number") return { doubleValue: value };
  throw new Error(`Tipo no soportado: ${typeof value}`);
}

function buildChatId(uidA, uidB) {
  return uidA.localeCompare(uidB) <= 0 ? `${uidA}_${uidB}` : `${uidB}_${uidA}`;
}

function countFirst(pairs) {
  const map = new Map();
  for (const [first] of pairs) map.set(first, (map.get(first) || 0) + 1);
  return map;
}

function countSecond(pairs) {
  const map = new Map();
  for (const [, second] of pairs) map.set(second, (map.get(second) || 0) + 1);
  return map;
}

function countBy(items, getter) {
  const map = new Map();
  for (const item of items) {
    const key = getter(item);
    map.set(key, (map.get(key) || 0) + 1);
  }
  return map;
}

function minutes(n) {
  return n * 60 * 1000;
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});

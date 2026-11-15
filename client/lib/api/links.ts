import { LinkItem, CreateLinkFormData } from "@/lib/link-schemas";

const BACKEND_INTERNAL_URL =
  process.env.BACKEND_INTERNAL_URL || "http://localhost:8080";
const SHORT_BASE_URL =
  process.env.NEXT_PUBLIC_SHORT_BASE_URL || "http://localhost:8080";

/**
 * Fetches the user's real links from Spring Boot GET /api/v1/links/me.
 * If user has 0 links, returns [] cleanly so the Empty State is displayed.
 */
export async function getLinksApi(token?: string): Promise<LinkItem[]> {
  if (!token) {
    return [];
  }

  try {
    const res = await fetch(`${BACKEND_INTERNAL_URL}/api/v1/links/me`, {
      method: "GET",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      cache: "no-store",
    });

    if (res.ok) {
      const backendList = (await res.json()) as Array<{
        shortCode: string;
        shortUrl: string;
        longUrl: string;
        isCustom: boolean;
        isActive: boolean;
        createdAt: string;
        expiresAt?: string | null;
      }>;

      if (Array.isArray(backendList)) {
        return await Promise.all(
          backendList.map(async (item) => {
            let clickCount = 0;
            try {
              const analyticsRes = await fetch(
                `${BACKEND_INTERNAL_URL}/api/v1/links/${item.shortCode}/analytics`,
                {
                  headers: { Authorization: `Bearer ${token}` },
                  cache: "no-store",
                }
              );
              if (analyticsRes.ok) {
                const analyticsData = await analyticsRes.json();
                clickCount = Number(analyticsData.totalClicks) || 0;
              }
            } catch {
              // Fallback to 0 if analytics not yet aggregated
            }

            return {
              ...item,
              shortUrl: `${SHORT_BASE_URL}/${item.shortCode}`,
              clickCount,
              isSampleData: false,
            };
          })
        );
      }
    }

    return [];
  } catch (err) {
    console.warn("Backend link fetch error:", err);
    return [];
  }
}

/**
 * Creates a short link on Spring Boot POST /api/v1/links.
 */
export async function createLinkApi(
  data: CreateLinkFormData,
  token?: string
): Promise<LinkItem> {
  if (!token) {
    throw new Error("You must be signed in to create a link");
  }

  const res = await fetch(`${BACKEND_INTERNAL_URL}/api/v1/links`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      longUrl: data.longUrl,
      customAlias: data.customAlias?.trim() || null,
      expiresAt: data.expiresAt || null,
    }),
    cache: "no-store",
  });

  const body = await res.json().catch(() => null);

  if (!res.ok) {
    const errorMsg =
      body?.detail || body?.message || "Failed to create short link";
    throw new Error(errorMsg);
  }

  return {
    shortCode: body.shortCode,
    shortUrl: `${SHORT_BASE_URL}/${body.shortCode}`,
    longUrl: body.longUrl,
    isCustom: body.isCustom ?? Boolean(data.customAlias),
    isActive: body.isActive ?? true,
    createdAt: body.createdAt || new Date().toISOString(),
    expiresAt: body.expiresAt || null,
    clickCount: 0,
    isSampleData: false,
  };
}

/**
 * Deletes a link on Spring Boot DELETE /api/v1/links/{shortCode}.
 */
export async function deleteLinkApi(
  shortCode: string,
  token?: string
): Promise<void> {
  if (!token) {
    throw new Error("Authentication required");
  }

  const res = await fetch(
    `${BACKEND_INTERNAL_URL}/api/v1/links/${shortCode}`,
    {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${token}`,
      },
      cache: "no-store",
    }
  );

  if (!res.ok && res.status !== 204) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.detail || body?.message || "Failed to delete link");
  }
}

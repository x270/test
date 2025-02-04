import React, { useState, useEffect } from "react";
import { Heart } from "lucide-react";
import { supabase } from "../../lib/supabase";
import type { Product } from "../../types";

export function ProductList() {
  const [products, setProducts] = useState<Product[]>([]);
  const [favorites, setFavorites] = useState<Set<string>>(new Set());
  const [searchTerm, setSearchTerm] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [totalProducts, setTotalProducts] = useState(0);
  const productsPerPage = 10;

  useEffect(() => {
    loadProducts();
    loadFavorites();
  }, [currentPage, searchTerm]);

  const loadProducts = async () => {
    let query = supabase.from("products").select("*", { count: "exact" });

    if (searchTerm) {
      query = query.or(
        `name.ilike.%${searchTerm}%,description.ilike.%${searchTerm}%`
      );
    }

    const { data, count, error } = await query
      .range(
        (currentPage - 1) * productsPerPage,
        currentPage * productsPerPage - 1
      )
      .order("created_at", { ascending: false });

    if (error) {
      console.error("Error loading products:", error);
      return;
    }

    setProducts(data || []);
    setTotalProducts(count || 0);
  };

  const loadFavorites = async () => {
    const {
      data: { user },
    } = await supabase.auth.getUser();
    if (!user) return;

    const { data, error } = await supabase
      .from("favorites")
      .select("product_id")
      .eq("user_id", user.id);

    if (error) {
      console.error("Error loading favorites:", error);
      return;
    }

    setFavorites(new Set(data.map((f) => f.product_id)));
  };

  const toggleFavorite = async (productId: string) => {
    const {
      data: { user },
    } = await supabase.auth.getUser();
    if (!user) return;

    if (favorites.has(productId)) {
      await supabase
        .from("favorites")
        .delete()
        .match({ user_id: user.id, product_id: productId });

      setFavorites((prev) => {
        const next = new Set(prev);
        next.delete(productId);
        return next;
      });
    } else {
      await supabase
        .from("favorites")
        .insert({ user_id: user.id, product_id: productId });

      setFavorites((prev) => new Set([...prev, productId]));
    }
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <input
          type="text"
          placeholder="Search products..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      {searchTerm && (
        <div className="mb-4 text-gray-600">
          {totalProducts > 0
            ? `Search results for "${searchTerm}": ${totalProducts} products`
            : `No products found for "${searchTerm}"`}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {products.map((product) => (
          <div
            key={product.id}
            className="bg-white rounded-lg shadow-md overflow-hidden"
          >
            <img
              src={product.image_url}
              alt={product.name}
              className="w-full h-48 object-cover"
            />
            <div className="p-4">
              <div className="flex justify-between items-start">
                <a
                  href={`/product/${product.id}`}
                  className="text-xl font-semibold hover:text-blue-600"
                >
                  {product.name}
                </a>
                <button
                  onClick={() => toggleFavorite(product.id)}
                  className={`p-2 rounded-full ${
                    favorites.has(product.id)
                      ? "text-red-500"
                      : "text-gray-400 hover:text-red-500"
                  }`}
                >
                  <Heart
                    size={24}
                    fill={favorites.has(product.id) ? "currentColor" : "none"}
                  />
                </button>
              </div>
              <p className="text-gray-600 mt-2">{product.description}</p>
              <p className="text-xl font-bold text-blue-600 mt-4">
                ${product.price.toFixed(2)}
              </p>
            </div>
          </div>
        ))}
      </div>

      {totalProducts > productsPerPage && (
        <div className="mt-8 flex justify-center space-x-2">
          {Array.from(
            { length: Math.ceil(totalProducts / productsPerPage) },
            (_, i) => i + 1
          ).map((page) => (
            <button
              key={page}
              onClick={() => setCurrentPage(page)}
              className={`px-4 py-2 rounded-lg ${
                currentPage === page
                  ? "bg-blue-500 text-white"
                  : "bg-gray-200 text-gray-700 hover:bg-gray-300"
              }`}
            >
              {page}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

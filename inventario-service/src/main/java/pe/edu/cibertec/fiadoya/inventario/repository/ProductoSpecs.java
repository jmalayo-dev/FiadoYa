package pe.edu.cibertec.fiadoya.inventario.repository;

import org.springframework.data.jpa.domain.Specification;
import pe.edu.cibertec.fiadoya.inventario.entity.Categoria;
import pe.edu.cibertec.fiadoya.inventario.entity.Producto;

/** Filtros de GET /api/productos: q, soloBajos, categoria y codigo. */
public final class ProductoSpecs {

    private ProductoSpecs() {
    }

    public static Specification<Producto> activos() {
        return (root, query, cb) -> cb.isTrue(root.get("activo"));
    }

    public static Specification<Producto> texto(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String patron = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("nombre")), patron),
                cb.like(root.get("codigoBarras"), patron));
    }

    public static Specification<Producto> soloBajos(Boolean soloBajos) {
        if (!Boolean.TRUE.equals(soloBajos)) {
            return null;
        }
        return (root, query, cb) -> cb.le(root.get("stock"), root.get("stockMinimo"));
    }

    public static Specification<Producto> categoria(Categoria categoria) {
        return categoria == null ? null : (root, query, cb) -> cb.equal(root.get("categoria"), categoria);
    }

    public static Specification<Producto> codigo(String codigo) {
        return codigo == null || codigo.isBlank() ? null
                : (root, query, cb) -> cb.equal(root.get("codigoBarras"), codigo.trim());
    }
}

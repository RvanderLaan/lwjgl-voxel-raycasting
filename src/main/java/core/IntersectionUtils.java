package core;

import geometry.Plane;
import geometry.Sphere;
import org.joml.Vector3f;

public class IntersectionUtils {
    Vector3f temp = new Vector3f();
    float plane_distance(Plane p, Vector3f point) {
        return point.sub(p.position, temp).dot(p.direction);
    }

    boolean sphere_inside_plane(Sphere s, Plane p) {
        return -plane_distance(p, s.getOrigin()) > s.getRadius();
    }

    boolean sphere_outside_plane(Sphere s, Plane p) {
        return plane_distance(p, s.getOrigin()) > s.getRadius();
    }

    boolean sphere_intersects_plane(Sphere s, Plane p) {
        return Math.abs(plane_distance(p, s.getOrigin())) <= s.getRadius();
    }

//    boolean sphere_inside_box(Sphere s, box b) {
//
//        if (!sphere_inside_plane(s, b.front))  { return false; }
//        if (!sphere_inside_plane(s, b.back))   { return false; }
//        if (!sphere_inside_plane(s, b.top))    { return false; }
//        if (!sphere_inside_plane(s, b.bottom)) { return false; }
//        if (!sphere_inside_plane(s, b.left))   { return false; }
//        if (!sphere_inside_plane(s, b.right))  { return false; }
//
//        return true;
//
//    }
//
//    boolean sphere_intersects_box(Sphere s, box b) {
//
//        boolean in_left   = !sphere_outside_plane(s, b.left);
//        boolean in_right  = !sphere_outside_plane(s, b.right);
//        boolean in_front  = !sphere_outside_plane(s, b.front);
//        boolean in_back   = !sphere_outside_plane(s, b.back);
//        boolean in_top    = !sphere_outside_plane(s, b.top);
//        boolean in_bottom = !sphere_outside_plane(s, b.bottom);
//
//        if (sphere_intersects_plane(s, b.top) &&
//                in_left && in_right && in_front && in_back) {
//            return true;
//        }
//
//        if (sphere_intersects_plane(s, b.bottom) &&
//                in_left && in_right && in_front && in_back) {
//            return true;
//        }
//
//        if (sphere_intersects_plane(s, b.left) &&
//                in_top && in_bottom && in_front && in_back) {
//            return true;
//        }
//
//        if (sphere_intersects_plane(s, b.right) &&
//                in_top && in_bottom && in_front && in_back) {
//            return true;
//        }
//
//        if (sphere_intersects_plane(s, b.front) &&
//                in_top && in_bottom && in_left && in_right) {
//            return true;
//        }
//
//        if (sphere_intersects_plane(s, b.back) &&
//                in_top && in_bottom && in_left && in_right) {
//            return true;
//        }
//
//        return false;
//    }
//
//    boolean sphere_outside_box(Sphere s, box b) {
//        return !(sphere_inside_box(s, b) || sphere_intersects_box(s, b));
//    }


}
